package client.timeline;

import client.UserSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PollData {
    private final List<String> choices;
    private final List<Integer> votes;
    private final Map<String, Integer> votesByUser;
    private final Instant endsAt;

    public PollData(List<String> choices, Instant endsAt) {
        this(choices, choices.stream().map(ignored -> 0).toList(), Map.of(), endsAt);
    }

    public PollData(List<String> choices, List<Integer> votes, Map<String, Integer> votesByUser, Instant endsAt) {
        this.choices = new ArrayList<>(choices);
        this.votes = new ArrayList<>(votes);
        this.votesByUser = new HashMap<>();
        votesByUser.forEach((user, choice) -> this.votesByUser.put(normalize(user), choice));
        this.endsAt = endsAt;
    }

    public List<String> getChoices() { return List.copyOf(choices); }
    public List<Integer> getVotes() { return List.copyOf(votes); }
    public Map<String, Integer> getVotesByUser() { return Map.copyOf(votesByUser); }
    public Instant getEndsAt() { return endsAt; }
    public int getTotalVotes() { return votes.stream().mapToInt(Integer::intValue).sum(); }
    public boolean isEnded() { return !Instant.now().isBefore(endsAt); }
    public boolean hasVoted() { return votesByUser.containsKey(activeUsername()); }
    public Integer activeChoice() { return votesByUser.get(activeUsername()); }

    public boolean vote(int index) {
        String actor = activeUsername();
        if (isEnded() || votesByUser.containsKey(actor) || index < 0 || index >= choices.size()) return false;
        votesByUser.put(actor, index);
        votes.set(index, votes.get(index) + 1);
        return true;
    }

    private static String activeUsername() {
        String username = UserSession.getInstance().getUsername();
        return normalize(username == null ? "anonymous" : username);
    }

    private static String normalize(String value) { return value.toLowerCase(Locale.ROOT); }
}
