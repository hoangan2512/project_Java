package model;

// lớp cơ sở
abstract class Entity {
    protected int id;
    protected String name;
    public Entity(int id, String name){
        this.id = id;
        this.name = name;
    }
    public Entity(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
