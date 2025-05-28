package app.jyu;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuizCraftTeam {
    private final Set<String> members;

    QuizCraftTeam(){
        this.members = new HashSet<>();
    }

    QuizCraftTeam(Set<String> members){
        this.members = members;
    }

    QuizCraftTeam(String... ids){
        this.members = new HashSet<>(List.of(ids));
    }

    public Set<String> getMembers() {
        return members;
    }

    public boolean add(String id){
        return members.add(id);
    }

    public boolean contains(String id){
        return members.contains(id);
    }
}
