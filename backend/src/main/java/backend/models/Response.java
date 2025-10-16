package backend.models;

import java.time.LocalTime;
import java.util.Map;

public class Response {

    private final Pair<Long, LocalTime>[] bestSolution;
    private final Map<Pair<Long, Long>, Map<Long, Long>> predecesseurs;

    public Response(Pair<Long, LocalTime>[] bestSolution, Map<Pair<Long, Long>, Map<Long, Long>> predecesseurs) {
        this.bestSolution = bestSolution;
        this.predecesseurs = predecesseurs;
    }

    public Pair<Long, LocalTime>[] getBestSolution() {
        return bestSolution;
    }

    public Map<Pair<Long, Long>, Map<Long, Long>> getPredecesseurs() {
        return predecesseurs;
    }
    
}
