package com.swarmus.hivear.viewmodels;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.MessageOuterClass;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;

public class ProtoMsgViewModel extends ViewModel {
    // First element = most recent
    private final MutableLiveData<LinkedList<String>> msgQueue = new MutableLiveData<>(new LinkedList<>());
    private final int maxCapacity = 10; // We keep last 10 messages
    private final static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    private static String requestPattern = "request";
    private static String responsePattern = "response";
    private static String greetingPattern = "greeting";
    private static String nullPattern = "null";

    private static int requestColor = Color.BLUE;
    private static int responseColor = Color.GREEN;
    private static int greetingColor = Color.YELLOW;
    private static int invalidColor = Color.RED;

    public LiveData<LinkedList<String>> getMsgQueue() { return msgQueue; }

    public void addMsg(MessageOuterClass.Message msg) {
        LinkedList<String> msgs = new LinkedList<>(msgQueue.getValue());
        if (msgs.size() == maxCapacity) { msgs.removeLast(); }
        String msgString = "Received at: " + sdf.format(Calendar.getInstance().getTime()) + "\n";
        msgString += msg.toString();
        msgs.addFirst(msgString);
        msgQueue.setValue(new LinkedList<>(msgs));
    }

    public void clearMsgs() {
        msgQueue.setValue(new LinkedList<>());
    }

    public String getLastMsgs(int nbToRetrieve) {
        LinkedList<String> msgs = msgQueue.getValue();
        nbToRetrieve = Math.min(nbToRetrieve, msgs.size());
        nbToRetrieve = Math.max(nbToRetrieve, 0);
        String logMsg = "";
        for (int i = 0; i < nbToRetrieve; i++) {
            logMsg += "\n==================================\n";
            logMsg += msgs.get(i) + "\n";
        }
        return logMsg;
    }

    public SpannableString getLastMsgsSpannable(int nbToRetrieve) {

        String lastMsgs = getLastMsgs(nbToRetrieve);

        SpannableString s = new SpannableString(lastMsgs);
        String[] parts = lastMsgs.split("\n");
        int lastResponse = 0;
        int lastRequest = 0;
        int lastGreeting = 0;
        int lastNull = 0;
        for(final String word : parts) {
            if(word.contains(responsePattern)) {
                s.setSpan(new ForegroundColorSpan(responseColor),
                        lastMsgs.indexOf(responsePattern, lastResponse),
                        lastMsgs.indexOf(responsePattern, lastResponse) + responsePattern.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                lastResponse = lastMsgs.indexOf(responsePattern, lastResponse)+1;
            } else if(word.contains(requestPattern)) {
                s.setSpan(new ForegroundColorSpan(requestColor),
                        lastMsgs.indexOf(requestPattern, lastRequest),
                        lastMsgs.indexOf(requestPattern, lastRequest) + requestPattern.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                lastRequest = lastMsgs.indexOf(requestPattern, lastRequest)+1;
            } else if(word.contains(greetingPattern)) {
                s.setSpan(new ForegroundColorSpan(greetingColor),
                        lastMsgs.indexOf(greetingPattern, lastGreeting),
                        lastMsgs.indexOf(greetingPattern, lastGreeting) + greetingPattern.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                lastGreeting = lastMsgs.indexOf(greetingPattern, lastGreeting)+1;
            } else if(word.contains(nullPattern)) {
                s.setSpan(new ForegroundColorSpan(invalidColor),
                        lastMsgs.indexOf(nullPattern, lastNull),
                        lastMsgs.indexOf(nullPattern, lastNull) + nullPattern.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                lastNull = lastMsgs.indexOf(nullPattern, lastNull)+1;
            }
        }
        return s;
    }
}
