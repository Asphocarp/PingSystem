package app.jyu;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SophisticatedPingTeam {
    private final Set<String> members;

    SophisticatedPingTeam(){
        this.members = new HashSet<>();
    }

    SophisticatedPingTeam(Set<String> members){
        this.members = members;
    }

    SophisticatedPingTeam(String... ids){
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
