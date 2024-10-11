package com.example.bpt;

public class Part {
    private String partName;
    private double totalDistance;

    // Konstruktor
    public Part(String partName, double totalDistance) {
        this.partName = partName;
        this.totalDistance = totalDistance;
    }

    // Getterek
    public String getPartName() {
        return partName;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    // Szükség esetén beállíthatsz settereket is, ha módosítani kell az értékeket
    public void setPartName(String partName) {
        this.partName = partName;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }
}
