package client;

import com.zaxxer.hikari.HikariDataSource;
import lib.ORManager;
import lib.ORManagerImpl;

public class Main {


    public static void main(String[] args) {
        ORManagerImpl orManager = (ORManagerImpl) ORManager.withDataSource(new HikariDataSource());
    }
}
