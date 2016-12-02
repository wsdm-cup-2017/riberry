package com.rf.categ;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Base64;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;


public class AggregateFeatures {

    private static final String [] FILE_NAMES = {
        //"aggregate_ui_01.txt",
        "user_item.txt",
        "user_credit.txt",
        "item_user.txt",
        "item_credit.txt"};


    private List<Map<Integer, ?>> maps;
    private List<AggregateFeature> features;
    private String path;

    /**
     * @param path      the path to the map data directory that contains the 
     * five txt files as specified in FILE_NAMES array;
     */
    public AggregateFeatures(String path) {
        this.path = path;
        maps = new ArrayList<>(FILE_NAMES.length);

        Map<Integer, String> userItemMap = new HashMap<>();
        Map<Integer, UserInfo> userInfoMap = new HashMap<>();
        Map<Integer, ItemInfo> itemInfoMap = new HashMap<>();

        features = new ArrayList<>(6);
        //features.add(new UserItemCredit());
        //maps.add(userItemMap);        

        features.add(new UserRevCount());
        maps.add(userInfoMap);

        features.add(new UserCredit());
        maps.add(userInfoMap);

        features.add(new ItemUserCount());
        maps.add(itemInfoMap);

        features.add(new ItemCredit());
        maps.add(itemInfoMap);

        Map<Integer, ?> curMap = null;
        AggregateFeature curFeature = null;
        for (int i = 0; i < FILE_NAMES.length; ++i) {
            curMap = maps.get(i);
            curFeature = features.get(i);

            String filename = path + "/" + FILE_NAMES[i];
            System.out.println("loading " + filename);
            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                String line = br.readLine();
                Integer key = null;
                String value = null;
                while(line != null) {
                    int idx = line.lastIndexOf(":");
                    key = curFeature.compressKey(line.substring(0, idx));
                    if (key == null) {
                        continue;
                    }
                    value = line.substring(idx + 1);

                    curFeature.addValue(key, value, curMap);

                    line = br.readLine();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }



        // day of the week

        maps.add(null);
        features.add(new DayOfWeek());
    }

    public String appendAggregateFeatures(String featureStr) {
        String[] records = featureStr.split(",");

        assert (records.length > 8) : "input string contains less than 9 features";

        String appendFeatures = "";

        for (int i = 0; i < FILE_NAMES.length + 1; ++i) {
            appendFeatures += "," + features.get(i).getFeature(records, maps.get(i));
        }
        return featureStr + "," + UserItemCredit.DEFAULT_VALUE + appendFeatures;
    }

    public static String intToStr(int number) {
        byte[] bytes = new byte[4];

        bytes[0] = (byte) (number >> 24);
        bytes[1] = (byte) (number >> 16);
        bytes[2] = (byte) (number >> 8);
        bytes[3] = (byte) (number);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String ipToStr(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return Base64.getEncoder().encodeToString(addr.getAddress());
        } catch (Exception ex) {
            System.out.println("cannot parse ip " + ip);
            return "";
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////    
    //                           Feature Implementations                              //
    ////////////////////////////////////////////////////////////////////////////////////

    private interface AggregateFeature {
        String getFeature(String[] records, Map<Integer, ?> data);
        void addValue(Integer key, String value, Map<Integer, ?> data);
        Integer compressKey(String key);
    }


    private class UserItemCredit implements AggregateFeature {
        public static final String DEFAULT_VALUE = "0,NA";

        @Override
        public String getFeature(String[] records, Map<Integer, ?> untypedData) {
            try {
                @SuppressWarnings("unchecked")
                Map<Integer, String> data = (Map<Integer, String>) untypedData;

                String userId = records[1];
                String userName = records[2];
                String itemId = records[8];
                
                Integer key = compressKey(userId + "," + userName + "," + itemId + ",");
                if (!data.containsKey(key)) {
                    return DEFAULT_VALUE;
                }

                String value = data.get(key);
                if (value == null) {
                    return DEFAULT_VALUE;
                }

                String [] items = value.split(",");
                assert (items.length != 2): 
                    "map data parse error in UserExpertise feature: key = " + key  + ": value = " + value + "\n";
                if (items.length != 2) {
                    return DEFAULT_VALUE;
                }


                int goodRev = Integer.parseInt(items[0]);
                int badRev = Integer.parseInt(items[1]);
                float goodRatio = goodRev * 1.0F / (goodRev + badRev);
                return (goodRev + badRev) + "," + ((String) String.format("%.3f", goodRatio));
            } catch (Exception ex) {
                return DEFAULT_VALUE;
            }
        }

        @Override
        public void addValue(Integer key, String value, Map<Integer, ?> untypedData) {
            @SuppressWarnings("unchecked")
            Map<Integer, String> data = (Map<Integer, String>) untypedData;
            data.put(key, value);
        }

        @Override
        public Integer compressKey(String key) {
            /*
            String[] items = key.split(",");
            if (items.length < 3) {
                return null;
            }

            Integer uid = Integer.parseInt(items[0]);
            int iid = Integer.parseInt(items[2]);

            return (uid << 32) + iid;
            */
            return 0;
        }
    }

    private class UserRevCount implements AggregateFeature {
        private static final String DEFAULT_VALUE = "0";

        @Override
        public String getFeature(String[] records, Map<Integer, ?> untypedData) {
            try {
                @SuppressWarnings("unchecked")
                Map<Integer, UserInfo> data = (Map<Integer, UserInfo>) untypedData;

                String userId = records[1];
                String userName = records[2];
                Integer key = compressKey(userId + "," + userName + ",");

                if (!data.containsKey(key)) {
                    return DEFAULT_VALUE;
                }

                return String.valueOf(data.get(key).itemRev);
            } catch (Exception ex) {
                return DEFAULT_VALUE;
            }
        }

        @Override
        public void addValue(Integer key, String value, Map<Integer, ?> untypedData) {
            @SuppressWarnings("unchecked")
            Map<Integer, UserInfo> data = (Map<Integer, UserInfo>) untypedData;

            UserInfo info = null;
            if (data.containsKey(key)) {
                info = data.get(key);
            } else {
                info = new UserInfo();
            }

            info.itemRev = Integer.parseInt(value);
            data.put(key, info);
        }

        @Override
        public Integer compressKey(String key) {
            String[] items = key.split(",");
            if (items.length < 2) {
                return null;
            }

            return Integer.parseInt(items[0]);
        }
    }

    private class UserCredit implements AggregateFeature {
        private static final String DEFAULT_VALUE = "0,NA";

        @Override
        public String getFeature(String[] records, Map<Integer, ?> untypedData) {
            try {
                @SuppressWarnings("unchecked")
                Map<Integer, UserInfo> data = (Map<Integer, UserInfo>) untypedData;

                String userId = records[1];
                String userName = records[2];
                Integer key = compressKey(userId + "," + userName + ",");

                if (!data.containsKey(key)) {
                    return DEFAULT_VALUE;
                }

                UserInfo value = data.get(key);

                int goodRev = value.goodRev;
                int badRev = value.badRev;
                if (goodRev + badRev <= 0) {
                    return DEFAULT_VALUE;
                }

                float goodRatio = goodRev * 1.0F / (goodRev + badRev);
                return (goodRev + badRev) + "," + ((String) String.format("%.3f", goodRatio));
            } catch (Exception ex) {
                return DEFAULT_VALUE;
            }
        }

        @Override
        public void addValue(Integer key, String value, Map<Integer, ?> untypedData) {
            @SuppressWarnings("unchecked")
            Map<Integer, UserInfo> data = (Map<Integer, UserInfo>) untypedData;

            UserInfo info = null;
            if (data.containsKey(key)) {
                info = data.get(key);
            } else {
                info = new UserInfo();
            }

            String [] items = value.split(",");
            info.goodRev = Integer.parseInt(items[0]);
            info.badRev = Integer.parseInt(items[1]);
            data.put(key, info);
        }

        @Override
        public Integer compressKey(String key) {
            String[] items = key.split(",");
            if (items.length < 2) {
                return null;
            }

            return Integer.parseInt(items[0]);
        }
    }

    private class ItemUserCount implements AggregateFeature {
        private static final String DEFAULT_VALUE = "0";

        @Override
        public String getFeature(String[] records, Map<Integer, ?> untypedData) {
            try {
                @SuppressWarnings("unchecked")
                Map<Integer, ItemInfo> data = (Map<Integer, ItemInfo>) untypedData;

                Integer key = compressKey(records[8]);

                if (!data.containsKey(key)) {
                    return DEFAULT_VALUE;
                }

                return String.valueOf(data.get(key).userRev);
            } catch (Exception ex) {
                return DEFAULT_VALUE;
            }
        }

        @Override
        public void addValue(Integer key, String value, Map<Integer, ?> untypedData) {
            @SuppressWarnings("unchecked")
            Map<Integer, ItemInfo> data = (Map<Integer, ItemInfo>) untypedData;

            ItemInfo info = null;
            if (data.containsKey(key)) {
                info = data.get(key);
            } else {
                info = new ItemInfo();
            }

            info.userRev = Integer.parseInt(value);
            data.put(key, info);
        }

        @Override
        public Integer compressKey(String key) {
            return Integer.parseInt(key);
        }
    }

    private class ItemCredit implements AggregateFeature {
        private static final String DEFAULT_VALUE = "0,NA";

        @Override
        public String getFeature(String[] records, Map<Integer, ?> untypedData) {
            try {
                @SuppressWarnings("unchecked")
                Map<Integer, ItemInfo> data = (Map<Integer, ItemInfo>) untypedData;

                Integer key = compressKey(records[8]);

                if (!data.containsKey(key)) {
                    return DEFAULT_VALUE;
                }

                ItemInfo value = data.get(key);

                int goodRev = value.goodRev;
                int badRev = value.badRev;
                if (goodRev + badRev <= 0) {
                    return DEFAULT_VALUE;
                }

                float goodRatio = goodRev * 1.0F / (goodRev + badRev);
                return (goodRev + badRev) + "," + ((String) String.format("%.3f", goodRatio));
            } catch (Exception ex) {
                return DEFAULT_VALUE;
            }
        }

        @Override
        public void addValue(Integer key, String value, Map<Integer, ?> untypedData) {
            @SuppressWarnings("unchecked")
            Map<Integer, ItemInfo> data = (Map<Integer, ItemInfo>) untypedData;

            ItemInfo info = null;
            if (data.containsKey(key)) {
                info = data.get(key);
            } else {
                info = new ItemInfo();
            }

            String [] items = value.split(",");
            info.goodRev = Integer.parseInt(items[0]);
            info.badRev = Integer.parseInt(items[1]);
            data.put(key, info);
        }

        @Override
        public Integer compressKey(String key) {
            return Integer.parseInt(key);
        }
    }

    private class DayOfWeek implements AggregateFeature {
        SimpleDateFormat dateFormat;
        SimpleDateFormat dayOfWeekFormat;

        public DayOfWeek() {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            dayOfWeekFormat = new SimpleDateFormat("u");
        }

        @Override
        public String getFeature(String[] records, Map<Integer, ?> untypedData) {
            @SuppressWarnings("unchecked")
            Map<Integer, String> data = (Map<Integer, String>) untypedData;

            String ts = records[4].replace("T", " ").replace("Z", "");

            try {
                Date date = dateFormat.parse(ts);
                return "" + (Integer.parseInt(dayOfWeekFormat.format(date)) - 1);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void addValue(Integer key, String value, Map<Integer, ?> untypedData) {
            throw new IllegalStateException("Should never access DayOfWeek.addValue");
        }

        @Override
        public Integer compressKey(String key) {
            throw new IllegalStateException("Should never access DayOfWeek.compressKey");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////    
    //                           Feature Implementations                              //
    ////////////////////////////////////////////////////////////////////////////////////

    private class ItemInfo {
        public int goodRev = -1;
        public int badRev = -1;
        public int userRev = 0;
    }

    private class UserInfo {
        public int goodRev = -1;
        public int badRev = -1;
        public int itemRev = 0;
    }

    public static void main(String [] args) {
        AggregateFeatures af = new AggregateFeatures("./data/python_and_map/");
        String input = "16,713,Denny,16,2012-10-29T17:03:21Z,NA,MISC,NA,15,Africa,NA,NA,Africa,"
            + "-1,-1,-1,-1,-1,NA,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,NA,F,F,NA,NA,NA,0,0.5,NA,"
            + "0,NA,NA,NA,NA,NA,NA,NA,F,T,NA,NA,NA,NA,NA,NA,F,F,NA,1,F,F,1,1,0,0,0,0,0,0,F,31,NA,"
            + "1,pageCreation,NA,NA,NA,179,NA,F,NA,NA,NA,NA,NA,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,"
            + "-1,-1,-1,-1,F,-1,-1,-1,-1,-1,-1,F,F,F,F,F,F,F,F,F,F";

        System.out.println(af.appendAggregateFeatures(input));
    }
}

