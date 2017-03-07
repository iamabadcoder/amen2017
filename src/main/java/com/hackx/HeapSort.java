package com.hackx;

/**
 * Created by hackx on 2/28/17.
 */
public class HeapSort {

    public static void main(String[] args) {
        int[] dataArr = {0, 16, 7, 3, 20, 17, 8};
        heapSort(dataArr, dataArr.length - 1);
        for (int num : dataArr) {
            System.out.println(num + ",");
        }
    }

    public static void heapSort(int[] data, int size) {
        buildHeap(data, size);
        for (int i = size; i >= 1; i--) {
            swap(data, 1, i);
            adjustHeap(data, 1, i - 1);
        }
    }


    public static void buildHeap(int[] data, int size) {

        for (int i = size / 2; i > 0; i--) {
            adjustHeap(data, i, size);
        }

    }

    public static void adjustHeap(int[] data, int i, int size) {
        int lchildIndex = i * 2;
        int rchildIndex = i * 2 + 1;
        int maxIndex = i;

        if (i <= size / 2) {
            if (lchildIndex <= size && data[lchildIndex] > data[i]) {
                maxIndex = lchildIndex;
            }
            if (rchildIndex <= size && data[rchildIndex] > data[i]) {
                maxIndex = rchildIndex;
            }
            if (maxIndex != i) {
                swap(data, maxIndex, i);
                adjustHeap(data, maxIndex, size);
            }
        }
    }

    public static void swap(int[] data, int i, int j) {
        int tmp = data[i];
        data[i] = data[j];
        data[j] = tmp;

    }


}
