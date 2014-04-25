package com.code9;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.util.List;

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

    public AnnotationFinder() {
        videoIds = new Hashtable<>();
        icarusIds = new Hashtable<>();
    }

    private void getAnnotations() {
        getArchivedData();

        Client c = ClientBuilder.newClient();
        WebTarget target = getTarget(c);

        String responseMsg = target.request(MediaType.TEXT_PLAIN_TYPE).get(
                        String.class);
        Gson gson = new Gson();
        ChannelList list = gson.fromJson(responseMsg, ChannelList.class);

        String nextPageToken = list.getNextPageToken();

        while (true) {
            List<Item> items = list.getItems();
            for (Item item : items) {
                Id id = item.getId();
                videoIds.put(id.getVideoId(), id.getVideoId());
            }
            
            if (nextPageToken == null || nextPageToken.equals(""))
                break;
            
            target = getTarget(c, nextPageToken);
            try {
                responseMsg = target.request(MediaType.TEXT_PLAIN_TYPE).get(String.class);
            list = gson.fromJson(responseMsg, ChannelList.class);
            nextPageToken = list.getNextPageToken();
            } catch (ServiceUnavailableException e) {
                break;
            }
        } 

        writeData();
    }

    private WebTarget getTarget(Client c, String nextPageToken) {
        WebTarget target = getTarget(c);
        target.queryParam("pageToken", nextPageToken);
        return target;
    }

    private WebTarget getTarget(Client c) {
        WebTarget target = c.target(BASE_URI);
        target = target.path("youtube/v3/search");
        target = target.queryParam("key",
                        "AIzaSyAGBmy6sp2E68-RRPYfsx03eZd88GPHjKI");
        target = target.queryParam("channelId", "UC_x5XG1OV2P6uZZ5FSM9Ttw")
                        .queryParam("part", "id");
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
            for (String o : videoIds.values())
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

    public static void main(String[] args) {
        AnnotationFinder af = new AnnotationFinder();
        af.getAnnotations();
    }
}
