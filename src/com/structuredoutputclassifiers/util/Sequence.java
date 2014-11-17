package com.structuredoutputclassifiers.util;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Author: Marcin Dobrowolski
 */
public class Sequence extends LinkedList<String> {

    private CharSequence separator = "";

    private Comparator<String> comparator = String.CASE_INSENSITIVE_ORDER;

    public Sequence(Sequence sequence) {
        super(sequence);
    }

    public Sequence(List<String> sequence) {
        super(sequence);
    }

    public Sequence() {
        super();
    }

    public void setSeparator(CharSequence separator) {
        this.separator = separator;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            sb.append(get(i));
            sb.append(separator);
        }
        return sb.toString().trim();
    }

    public Comparator<String> getComparator() {
        return comparator;
    }

    public void setComparator(Comparator<String> comparator) {
        this.comparator = comparator;
    }
}
