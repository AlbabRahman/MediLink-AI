package com.medilinkai.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Location helper for Bangladesh.
 * - geocode(): turns "Dhanmondi" into lat/lng using the free Nominatim API.
 * - distanceKm(): straight-line (Haversine) distance between two points.
 * Falls back to Dhaka centre when the network is unavailable.
 */
@Service
public class GeoService {

    /** Dhaka city centre, used when geocoding fails or is not requested. */
    public static final double DHAKA_LAT = 23.8103;
    public static final double DHAKA_LNG = 90.4125;

    private final RestTemplate restTemplate = new RestTemplate();

    /** Returns {lat, lng} for a place name in Bangladesh. */
    public double[] geocode(String place) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=bd&q="
                    + URLEncoder.encode(place, StandardCharsets.UTF_8);

            // Nominatim's fair-use policy requires a User-Agent header
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "MediLinkAI-student-project/1.0");
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            String body = response.getBody();
            if (body == null || body.equals("[]")) {
                return new double[]{DHAKA_LAT, DHAKA_LNG};
            }
            double lat = Double.parseDouble(extractJsonValue(body, "lat"));
            double lng = Double.parseDouble(extractJsonValue(body, "lon"));
            return new double[]{lat, lng};
        } catch (Exception e) {
            // offline or API problem -> Dhaka centre keeps the demo alive
            return new double[]{DHAKA_LAT, DHAKA_LNG};
        }
    }

    /** Haversine distance in kilometres between two lat/lng points. */
    public double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    /** Extracts the first "key":"value" pair from a small JSON array response. */
    private String extractJsonValue(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("key not found: " + key);
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
