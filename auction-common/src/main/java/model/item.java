package model;

//Item.java (Abstract Class): Chứa id, name, description, startingPrice, currentPrice, endTime, status.
//các lớp con Electronics.java, Art.java, Vehicle.java: Kế thừa từ Item để minh họa tính kế thừa rõ ràng.
public abstract class item extends entity {
}

class Electronics extends item {}


class Art extends item {}

class Vehicle extends item {}
