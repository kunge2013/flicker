package com.cratos.game.flicker;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

public class TestIterator {
	public static void main(String[] args) {
		LinkedBlockingQueue<String> list = new LinkedBlockingQueue<String>();
		list.add("A");
		list.add("B");
		list.add("C");
		list.add("D");
		list.add("E");
		Iterator<String> iterator = list.iterator();
		for (;iterator.hasNext();) {
			String value = iterator.next();
			if("A".equalsIgnoreCase(value)) list.remove("C");
			System.out.println(value);
		}
	}
}
