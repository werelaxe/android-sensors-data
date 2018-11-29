package com.example.werelaxe.sandbox;

public class SensorData {
    private static final int SIZE = 3;
    float[] accelerometerValues;
    float[] rotationValues;
    float[] magneticValues;

    String serialize() {
        StringBuilder result = new StringBuilder("[");
        for (float accelerometerValue: accelerometerValues) {
            result.append(accelerometerValue);
            result.append(",");
        }
        for (float rotationValue: rotationValues) {
            result.append(rotationValue);
            result.append(",");
        }
        for (int i = 0; i < SIZE - 1; i++) {
            result.append(magneticValues[i]);
            result.append(",");
        }
        result.append(magneticValues[SIZE - 1]);
        return result.toString() + "]";
    }

    boolean isReady() {
        return accelerometerValues != null && rotationValues != null && magneticValues != null;
    }
}
