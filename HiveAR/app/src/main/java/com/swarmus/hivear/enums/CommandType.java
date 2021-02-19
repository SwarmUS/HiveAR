package com.swarmus.hivear.enums;

import java.io.Serializable;

public enum CommandType implements Serializable {
    moveBy,
    doABackFlip; // TODO remove, only to test multiple commands

    public String toString() {
        switch (this) {
            case moveBy:
                return "moveBy";
            case doABackFlip:
                return "doABackFlip";
        }
        return "";
    }

    public static CommandType fromString(String cmdString) {
        for (CommandType cmd : CommandType.values()) {
            if (cmd.toString().equals(cmdString)) {
                return cmd;
            }
        }
        return null;
    }
}
