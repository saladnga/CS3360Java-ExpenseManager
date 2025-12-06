package com.expense;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles conversion between currencies.
 * Can use static exchange rates or live rates from
 * https://app.currencyapi.com/api-keys.
 */

public class CurrencyConverter {

    // Static fallback rates - from 2025 (in case API is unavailable)
    private static final Map<String, Double> STATIC_RATES = new HashMap<>();

    // Currency API key (placeholder)
    // Replace with your own API key
    private static final String API_KEY = "";

    static {
        STATIC_RATES.put("USD", 1.0);
        STATIC_RATES.put("EUR", 0.8635001386);
        STATIC_RATES.put("VND", 26382.005183193);
        STATIC_RATES.put("JPY", 155.5690267085);
        STATIC_RATES.put("GBP", 0.7606401215);
    }

    /**
     * Converts an amount from one currency to another.
     * Attempts to use live data from currencyapi.com; falls back to static rates if
     * API fails.
     *
     * @param amount   The amount to convert
     * @param fromCode Source currency code (e.g. "USD")
     * @param toCode   Target currency code (e.g. "EUR")
     * @return Converted amount
     */

    public double convertCurrency(double amount, String fromCode, String toCode) {

        // If you are using API, uncomment this:

        // try {
        // double liveRate = getLiveRate(fromCode, toCode);
        // if (liveRate > 0) {
        // return amount * liveRate;
        // }
        // } catch (Exception e) {
        // System.err.println("Live rate failed, using static fallbacks: " +
        // e.getMessage());
        // }

        if (STATIC_RATES.containsKey(fromCode) && STATIC_RATES.containsKey(toCode)) {
            double usdBase = amount / STATIC_RATES.get(fromCode);
            return usdBase * STATIC_RATES.get(toCode);
        }

        System.err.println("Unknown currency codes: " + fromCode + " or " + toCode);
        return amount;
    }

    /**
     * Fetches live currency rate from currencyapi.com.
     *
     * @param from Source currency (e.g. "USD")
     * @param to   Target currency (e.g. "EUR")
     * @return Exchange rate or -1 if unavailable
     */

    private double getLiveRate(String from, String to) {
        try {
            String urlStr = String.format(
                    "https://api.currencyapi.com/v3/latest?apikey=%s&currencies=%s,%s",
                    API_KEY, to, from);

            // Fix deprecated URL(String)
            URL url = URI.create(urlStr).toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int status = connection.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("HTTP Error: " + status);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            String json = response.toString();

            double rateFromUSD = extractValue(json, from);
            double rateToUSD = extractValue(json, to);

            if (rateFromUSD == 0)
                rateFromUSD = 1; // fallback

            return rateToUSD / rateFromUSD;

        } catch (Exception e) {
            System.err.println("Error fetching live rate: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Extracts rate from JSON string response.
     */
    private double extractValue(String json, String code) {
        try {
            String part = json.split("\"" + code + "\":")[1];
            return Double.parseDouble(part.split("\"value\":")[1].split("}")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

}
