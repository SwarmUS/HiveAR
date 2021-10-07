package com.swarmus.hivear.viewmodels;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.models.ProtoMsgStorer;

import java.util.ArrayList;
import java.util.List;

public class ProtoMsgViewModel extends ViewModel {

    private final MutableLiveData<ProtoMsgStorer> currentProtoMsgStorer;
    private final MutableLiveData<List<ProtoMsgStorer>> allProtoMsgStorer;

    private static String requestPattern = "request";
    private static String responsePattern = "response";
    private static String greetingPattern = "greeting";
    private static String nullPattern = "null";
    private static String destinationPattern = "destination";
    private static String sourcePattern = "source";

    private static int requestColor = Color.BLUE;
    private static int responseColor = Color.GREEN;
    private static int greetingColor = Color.YELLOW;
    private static int invalidColor = Color.RED;
    private static int destinationColor = Color.MAGENTA;
    private static int sourceColor = Color.CYAN;

    public ProtoMsgViewModel() {
        allProtoMsgStorer = new MutableLiveData<>(new ArrayList<>());
        currentProtoMsgStorer = new MutableLiveData<>();
    }

    public void registerNewProtoMsgStorer(ProtoMsgStorer protoMsgStorer) {
        ArrayList<ProtoMsgStorer> protoMsgStorers = new ArrayList<>(allProtoMsgStorer.getValue());
        protoMsgStorers.add(protoMsgStorer);
        allProtoMsgStorer.setValue(protoMsgStorers);
    }

    public void clearProtoMsgStorers() {
        clearLogs();
        allProtoMsgStorer.setValue(new ArrayList<>());
        currentProtoMsgStorer.setValue(null);
    }

    public LiveData<List<ProtoMsgStorer>> getProtoMsgStorerList() {return allProtoMsgStorer;}

    public LiveData<ProtoMsgStorer> getCurrentProtoMsgStorer() {
        return currentProtoMsgStorer;
    }

    public void setCurrentProtoMsgStorer(ProtoMsgStorer protoMsgStorer) {
        // Remove observer before changing ProtoMsgStorer
        if (currentProtoMsgStorer.getValue() != null) {
            currentProtoMsgStorer.getValue().deleteObservers();
        }
        currentProtoMsgStorer.setValue(protoMsgStorer);
    }

    public void clearLogs() {
        if (currentProtoMsgStorer != null && currentProtoMsgStorer.getValue() != null) {
            currentProtoMsgStorer.getValue().clear();
        }
    }

    public SpannableString getLastMsgsSpannable(int nbToRetrieve) {

        if (currentProtoMsgStorer == null || currentProtoMsgStorer.getValue() == null) {
            return new SpannableString("");
        }

        String lastMsgs = currentProtoMsgStorer.getValue().getLoggingString(nbToRetrieve);

        SpannableString s = new SpannableString(lastMsgs);
        String[] parts = lastMsgs.split("\n");
        int lastResponse = 0;
        int lastRequest = 0;
        int lastGreeting = 0;
        int lastNull = 0;
        int lastDestination = 0;
        int lastSource = 0;
        for(final String word : parts) {
            lastResponse =      colorizeString(responsePattern,     lastResponse,       responseColor,      word, lastMsgs, s);
            lastRequest =       colorizeString(requestPattern,      lastRequest,        requestColor,       word, lastMsgs, s);
            lastGreeting =      colorizeString(greetingPattern,     lastGreeting,       greetingColor,      word, lastMsgs, s);
            lastNull =          colorizeString(nullPattern,         lastNull,           invalidColor,       word, lastMsgs, s);
            lastDestination =   colorizeString(destinationPattern,  lastDestination,    destinationColor,   word, lastMsgs, s);
            lastSource =        colorizeString(sourcePattern,       lastSource,         sourceColor,        word, lastMsgs, s);
        }
        return s;
    }

    private int colorizeString(String pattern, int lastDetectionIndex, int color, String part, String text, SpannableString s) {
        if(part.contains(pattern)) {
            s.setSpan(new ForegroundColorSpan(color),
                    text.indexOf(pattern, lastDetectionIndex),
                    text.indexOf(pattern, lastDetectionIndex) + pattern.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            lastDetectionIndex = text.indexOf(pattern, lastDetectionIndex)+1;
        }
        return lastDetectionIndex;
    }
}
