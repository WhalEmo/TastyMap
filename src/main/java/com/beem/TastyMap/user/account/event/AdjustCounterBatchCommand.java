package com.beem.TastyMap.user.account.event;

import java.util.List;

public class AdjustCounterBatchCommand {

    public enum Action { INCREMENT, DECREMENT }
    public enum TargetField { SUBSCRIBER_COUNT, SUBSCRIBED_COUNT }

    private Long targetUserId;
    private List<Long> userIdsToUpdate;
    private Action action;
    private TargetField targetField;

    public AdjustCounterBatchCommand() {}

    public AdjustCounterBatchCommand(Long targetUserId, List<Long> userIdsToUpdate, Action action, TargetField targetField) {
        this.targetUserId = targetUserId;
        this.userIdsToUpdate = userIdsToUpdate;
        this.action = action;
        this.targetField = targetField;
    }

    public Long getTargetUserId() { return targetUserId; }
    public List<Long> getUserIdsToUpdate() { return userIdsToUpdate; }
    public Action getAction() { return action; }
    public TargetField getTargetField() { return targetField; }
}