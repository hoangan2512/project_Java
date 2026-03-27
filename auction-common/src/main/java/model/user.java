package model;

//User.java (Abstract Class): Chứa các thuộc tính chung như username, password, role.
public abstract class user extends entity {
}

class bidder extends user {}

class seller extends user {}

class admin extends user {}