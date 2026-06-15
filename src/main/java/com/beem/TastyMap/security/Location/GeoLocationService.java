package com.beem.TastyMap.security.Location;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

@Service
public class GeoLocationService {

    private final DatabaseReader databaseReader;

    public GeoLocationService() throws IOException {

        InputStream inputStream =
                getClass().getResourceAsStream(
                        "/geoip/GeoLite2-City.mmdb"
                );

        this.databaseReader =
                new DatabaseReader.Builder(inputStream)
                        .build();
    }

    public String getCity(String ipAddress) {

        try {

            if ("127.0.0.1".equals(ipAddress)
                    || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
                return "Local";
            }

            InetAddress ip =
                    InetAddress.getByName(ipAddress);

            CityResponse response =
                    databaseReader.city(ip);

            return response.getCity().getName();

        } catch (Exception e) {
            return "Unknown";
        }
    }
}