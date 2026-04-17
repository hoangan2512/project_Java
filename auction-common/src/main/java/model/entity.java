package model;

import java.io.Serializable;

// lớp cơ sở
abstract class entity implements Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected String name;

    public entity(int id, String name){
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
