package app.jyu;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PingSystemTeam {
    private final Set<String> members;

    PingSystemTeam(){
        this.members = new HashSet<>();
    }

    PingSystemTeam(Set<String> members){
        this.members = members;
    }

    PingSystemTeam(String... ids){
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
