package com.xabber.android.data.message;

import androidx.annotation.NonNull;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realmobjects.ForwardId;
import com.xabber.android.data.database.realmobjects.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.forward.ForwardComment;
import com.xabber.android.data.log.LogManager;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class ForwardManager {

    private static final String LOG_TAG = ForwardManager.class.getSimpleName();

    public static void forwardMessage(List<String> messages, AccountJid account, UserJid user, String text) {
        final AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        final MessageItem messageItem = chat.createNewMessageItem(text);

        RealmList<ForwardId> ids = new RealmList<>();

        for (String message : messages) {
            ids.add(new ForwardId(message));
        }

        messageItem.setForwardedIds(ids);

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                try {
                    realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealm(messageItem);
                            EventBus.getDefault().post(new NewMessageEvent());
                            chat.sendMessages();
                        }
                    });
                } catch (Exception e) { LogManager.exception(LOG_TAG, e); }
            }
        });
    }

    public static String parseForwardComment(Stanza packet) {
        ExtensionElement comment = packet.getExtension(ForwardComment.ELEMENT, ForwardComment.NAMESPACE);
        if (comment instanceof ForwardComment) {
            return ((ForwardComment) comment).getComment();
        }
        return null;
    }

    @NonNull
    public static List<Forwarded> getForwardedFromStanza(Stanza packet) {
        List<ExtensionElement> elements = packet.getExtensions(Forwarded.ELEMENT, Forwarded.NAMESPACE);
        if (elements == null || elements.size() == 0) return Collections.emptyList();

        List<Forwarded> forwarded = new ArrayList<>();
        for (ExtensionElement element : elements) {
            if (element instanceof Forwarded) {
                forwarded.add((Forwarded)element);
            }
        }
        return forwarded;
    }

}
