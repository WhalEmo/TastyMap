package com.beem.TastyMap.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssetLinksController {

    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAssetLinks() {
        String jsonBody = "[\n" +
                "  {\n" +
                "    \"relation\": [\"delegate_permission/common.handle_all_urls\"],\n" +
                "    \"target\": {\n" +
                "      \"namespace\": \"android_app\",\n" +
                "      \"package_name\": \"org.beem.tastymap\",\n" +
                "      \"sha256_cert_fingerprints\": [\"4C:92:C3:49:D2:F0:AC:68:02:67:B1:1C:A3:6F:67:17:7B:EA:03:EF:E8:71:0C:CA:56:A9:0E:3B:5C:0A:4E:06\"]\n" +
                "    }\n" +
                "  }\n" +
                "]";

        HttpHeaders headers = new HttpHeaders();
        headers.add("ngrok-skip-browser-warning", "true");

        return new ResponseEntity<>(jsonBody, headers, HttpStatus.OK);
    }
}