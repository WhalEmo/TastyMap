package com.beem.TastyMap.security.Location;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.servlet.http.HttpServletRequest;
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
                if (isLocalIp(ipAddress)) return "Local";
                InetAddress ip = InetAddress.getByName(ipAddress);
                CityResponse response = databaseReader.city(ip);
                return response.getCity() != null ? response.getMostSpecificSubdivision().getName() +" "+ response.getCity().getName() : "Unknown";
            } catch (Exception e) {
                System.out.println("GeolocatıonServıce " + e );
                return "Unknown";
            }
        }


        private boolean isLocalIp(String ip) {
            return ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") ||
                    ip.startsWith("172.") || ip.startsWith("192.168.") || ip.startsWith("10.");
        }

}