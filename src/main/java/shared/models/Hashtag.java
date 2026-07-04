package shared.models;

public class Hashtag {
    private int id;
    private String name;

    public Hashtag() {}

    public Hashtag(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}