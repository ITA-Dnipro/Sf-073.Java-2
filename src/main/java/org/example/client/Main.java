package org.example.client;

import com.zaxxer.hikari.HikariDataSource;
import org.example.lib.ORManager;
import org.example.lib.ORManagerImpl;

public class Main {


    public static void main(String[] args) {
        ORManagerImpl orManager = (ORManagerImpl) ORManager.withDataSource(new HikariDataSource());

    }
}
