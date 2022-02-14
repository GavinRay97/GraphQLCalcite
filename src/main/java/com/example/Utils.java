package com.example;

import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

public class Utils {
    static <T> Stream<T> reverse(List<T> input) {
        ListIterator<T> li = input.listIterator(input.size());
        return Stream.generate(li::previous).limit(input.size());
    }
}
