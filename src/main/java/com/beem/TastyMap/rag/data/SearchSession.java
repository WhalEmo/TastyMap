package com.beem.TastyMap.rag.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchSession implements Serializable {
    private String query;
    private List<String> placeIds; //vektorel dbden donen ıdler
    private int currentIndex;     //kullanıcya kac mekan gosterıldı
}