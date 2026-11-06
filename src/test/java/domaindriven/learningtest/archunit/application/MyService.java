package domaindriven.learningtest.archunit.application;

import domaindriven.learningtest.archunit.adapter.MyAdapter;

public class MyService {
    MyService2 myService2;

    void run() {
        myService2 = new MyService2();
        System.out.println(myService2);
    }
}
