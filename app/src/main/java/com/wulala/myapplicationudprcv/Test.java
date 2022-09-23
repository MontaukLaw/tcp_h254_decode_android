package com.wulala.myapplicationudprcv;

import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Test {
    public static void test() throws InterruptedException {

        final String TAG = Test.class.getSimpleName();
        // define capacity of BlockingQueue
        // 定义队列长度为4
        int capacityOfQueue = 4;

        // create object of BlockingQueue
        // new一个对象
        BlockingQueue<String> BQ = new LinkedBlockingQueue<String>(capacityOfQueue);

        // Add element to BlockingQueue
        // 加入3个element
        BQ.add("Ravi");
        BQ.add("Suraj");
        BQ.add("Harsh");

        // print elements of queue
        // 打印一下
        Log.d(TAG, "Items in Queue are " + BQ);

        // Try to poll elements from BQ
        // using poll(long timeout, TimeUnit unit) method
        // 使用poll方法, 从头取出一个
        // Removing item From head: Ravi
        Log.d(TAG, "Removing item From head: " + BQ.poll(5, TimeUnit.SECONDS));

        // print queue details
        // 打印 Now Queue Contains[Suraj, Harsh]
        Log.d(TAG, "Now Queue Contains" + BQ);

        // using poll(long timeout, TimeUnit unit) method
        // 使用poll方法, 再取出一个
        Log.d(TAG, "Removing item From head: " + BQ.poll(5, TimeUnit.SECONDS));

        // print queue details
        // 打印结果 Now Queue Contains[Harsh]
        Log.d(TAG, "Now Queue Contains" + BQ);

        // using poll(long timeout, TimeUnit unit) method
        // Removing item From head: Harsh
        // 取完之后, 已经全没了
        Log.d(TAG, "Removing item From head: " + BQ.poll(5, TimeUnit.SECONDS));

        // print queue details
        Log.d(TAG, "Now Queue Contains" + BQ);

        // using poll(long timeout, TimeUnit unit) method
        // 这个时候再取, 会等待5秒, 然后取到空值
        Log.d(TAG, "Removing item From head: " + BQ.poll(5, TimeUnit.SECONDS));

        // print queue details
        // 这个时候队列是空的.
        Log.d(TAG, "Now Queue Contains" + BQ);
    }

}
