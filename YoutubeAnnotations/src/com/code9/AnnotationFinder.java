package com.code9;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.code9.json.ChannelList;
import com.code9.json.Id;
import com.code9.json.Item;
import com.google.gson.Gson;

import edu.princeton.cs.introcs.In;
import edu.princeton.cs.introcs.Out;

public class AnnotationFinder {
    private final String BASE_URI = "https://www.googleapis.com";

    private Hashtable<String, String> videoIds;

    private Hashtable<String, String> icarusIds;

    private Random r = new Random(System.currentTimeMillis());

    private int idleTime = 0;

    public AnnotationFinder() {
        videoIds = new Hashtable<>();
        icarusIds = new Hashtable<>();
    }

    private void getVideoIds() {
        getArchivedData();

        Client c = ClientBuilder.newClient();
        WebTarget target = getChannelListTarget(c);

        String responseMsg = target.request(MediaType.TEXT_PLAIN_TYPE).get(
                String.class);
        Gson gson = new Gson();
        ChannelList list = gson.fromJson(responseMsg, ChannelList.class);

        String nextPageToken = list.getNextPageToken();

        int count = 0;

        while (true) {
            List<Item> items = list.getItems();
            for (Item item : items) {
                Id id = item.getId();
                if (id.getVideoId() != null) {
                    videoIds.put(id.getVideoId(), id.getVideoId());
                    System.out.println(++count + " of "
                            + list.getPageInfo().getTotalResults() + ": "
                            + id.getVideoId());
                }
            }

            if (nextPageToken == null)
                break;

            sleep();

            boolean tryRequest = true;

            while (tryRequest) {
                try {
                    c = ClientBuilder.newClient();
                    target = getChannelListTarget(c, nextPageToken);
                    responseMsg = target.request(MediaType.TEXT_PLAIN_TYPE)
                            .get(String.class);
                    list = gson.fromJson(responseMsg, ChannelList.class);
                    nextPageToken = list.getNextPageToken();
                    tryRequest = false;
                } catch (ServiceUnavailableException e) {
                    idleTime += 60000;
                    sleep(idleTime);
                }
            }
        }

        writeData();
    }

    private void sleep(int amount) {
        try {
            Thread.sleep(amount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sleep() {
        // sleep 100 ms to 350 ms
        sleep(r.nextInt(250) + 100);
    }

    private WebTarget getChannelListTarget(Client c, String nextPageToken) {
        WebTarget target = getChannelListTarget(c);
        target = target.queryParam("pageToken", nextPageToken);
        return target;
    }

    private WebTarget getChannelListTarget(Client c) {
        WebTarget target = c.target(BASE_URI);
        target = target.path("youtube/v3/search");
        // GoogleApiKey.key is a String that is your Google API Key
        // See https://developers.google.com/console/help/#WhatIsKey
        target = target.queryParam("key", GoogleApiKey.key);
        target = target.queryParam("channelId", "UC_x5XG1OV2P6uZZ5FSM9Ttw");
        target = target.queryParam("part", "id");
        target = target.queryParam("maxResults", 50);
        target = target.queryParam("order", "date");
        return target;
    }

    private void writeData() {
        File f = new File("videoIds.dat");
        FileOutputStream fos;
        Out out = null;
        try {
            fos = new FileOutputStream(f);
            out = new Out(fos);

            for (String o : videoIds.values())
                out.println(o);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } finally {
            if (out != null)
                out.close();
        }

        f = new File("icarusIds.dat");
        try {
            fos = new FileOutputStream(f);
            out = new Out(fos);
            for (String o : icarusIds.values())
                out.println(o);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } finally {
            if (out != null)
                out.close();
        }
    }

    private void getArchivedData() {
        File f = new File("videoIds.dat");
        if (f.exists()) {
            In in = new In(f);
            while (in.hasNextLine()) {
                String s = in.readLine();
                videoIds.put(s, s);
            }
            in.close();
        }

        f = new File("icarusIds.dat");
        if (f.exists()) {
            In in = new In(f);
            while (in.hasNextLine()) {
                String s = in.readLine();
                icarusIds.put(s, s);
            }
            in.close();
        }
    }

    private void getAnnotations() {
        String baseUri = "https://www.youtube.com/annotations_invideo?features=1&legacy=1&video_id=";
        for (String videoId : videoIds.values()) {
            InputStream is = null;
            try {
                URL url = new URL(baseUri + videoId);
                is = url.openStream(); // throws an IOException
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(is));

                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("<TEXT>")) {
                        int start = line.indexOf("<TEXT>");
                        int end = line.indexOf("</TEXT>", start);
                        if (start != -1 && end != -1 && start < end) {
                            String newUrl = line.substring(start + 6, end);
                            
                            if (newUrl.contains("goo.gl")) {
                                if (!newUrl.startsWith("http"))
                                    newUrl = "http://" + newUrl;
                                icarusIds.put(newUrl, newUrl);
//                                Desktop.getDesktop().browse(
//                                        new URI(newUrl));
//                                System.in.read();
                            }
                        }
                    }
                }
            } catch (MalformedURLException mue) {
                mue.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
//            } catch (URISyntaxException e) {
//                e.printStackTrace();
            } finally {
                try {
                    if (is != null)
                        is.close();
                } catch (IOException ioe) {
                    // nothing to see here
                }
            }
        }
        writeData();
    }

    public static void main(String[] args) {
        AnnotationFinder af = new AnnotationFinder();
        af.getVideoIds();
        af.getAnnotations();
    }
}
