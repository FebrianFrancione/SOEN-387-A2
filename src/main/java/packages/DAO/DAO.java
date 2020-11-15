
package packages.DAO;
import packages.businessLayer.Post;
import packages.businessLayer.User;
import packages.database.DBConnection;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;


//the direct access object that performs the CRUD operations
//a table for posts exists in the database with the columns: (postID, userID, text, attachmentSource, date, tags)
//a table for users exists in the database with the columns: (userID, username, password)
public class DAO {
    //insert user into user database table
    public boolean insert(User user) {
        Connection connection = DBConnection.getConnection();

        try {
            String query = "INSERT INTO users (userID,username, password, email) VALUES (?,?, ?, ?)";

            PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,"5");
            ps.setString(2,user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getEmail());

            int i = ps.executeUpdate();

            if(i == 1) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        System.out.println(generatedKeys.getInt(1));
                        System.out.println("Inserted User in User table successful!");
                    }
                    else {
                        throw new SQLException("Inserting user failed");
                    }
                }

                retrieveUserID(user);
                return true;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }

        return false;
    }

    //insert post into post database table
    public boolean insert(Post post) throws IOException {
        Connection connection = DBConnection.getConnection();
        try {
            String query = "INSERT INTO posts (userID, text, date, tags) VALUES (?, ?, ?, ?)";

            PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, post.getUserID());
            ps.setString(2, post.getText());
            //ps.setBlob(3, post.getAttachment());

//            java.sql.Date sqlDate = new java.sql.Date(post.getDate().getTime());

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            ps.setTimestamp(3, timestamp);
            ps.setString(4, post.getTags());

            int i = ps.executeUpdate();

            if(i == 1) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        System.out.println(generatedKeys.getInt(1));
                        System.out.println("Inserted Post in posts table successful!");
                    }
                    else {
                        throw new SQLException("Inserting user failed");
                    }
                }

                int id = retrievePostID(post);

                if (post.getAttachment().available() != 0) {
                    insertFile(id, post.getAttachment(), post.getOriginalFileName(), post.getFileSize(), post.getFileType());
                }
                return true;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }


        return false;
    }

    //insert file into files table
    public boolean insertFile(int id, InputStream inputStream, String fileName, long size, String type) {
        Connection connection = DBConnection.getConnection();

        try {
            String query = "INSERT INTO files (fileID, file, date, fileName, fileSize, fileType) VALUES (?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, id);
            ps.setBlob(2, inputStream);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setString(4, fileName);
            ps.setLong(5, size);
            ps.setString(6, type);

            int i = ps.executeUpdate();

            if(i == 1) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        System.out.println(generatedKeys.getInt(1));
                        System.out.println("Inserted file in files table successful!");
                    }
                    else {
                        throw new SQLException("Inserting file failed");
                    }
                }
                return true;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }

        return false;
    }

    //update user in the user table.
    public boolean update(User user) {
        Connection connection = DBConnection.getConnection();

        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE users SET username=?, password=?, email=? WHERE userID=" + user.getUserID());

            ps.setString(1,user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getEmail());

            int i = ps.executeUpdate();

            if(i == 1) {
                System.out.println("updated user successfully");
                return true;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }

        return false;
    }

    //update post in the post table.
    public boolean update(int userID, int postID, String text, InputStream inputStream, String tags) {
        Connection connection = DBConnection.getConnection();

        try {
            PreparedStatement ps = null;
            if (text.length() != 0 && inputStream.available() == 0 && tags.length() == 0) {
                ps = connection.prepareStatement("UPDATE posts SET text=?, lastUpdated=? WHERE postID=" + postID + " AND userID=" + userID);
                ps.setString(1, text);
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            } else if (text.length() != 0 && inputStream.available() != 0 && tags.length() == 0) {
                ps = connection.prepareStatement("UPDATE posts SET text=?, lastUpdated=? WHERE postID=" + postID + " AND userID=" + userID);
                ps.setString(1, text);
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                updateFile(postID, inputStream);
            } else if (text.length() != 0 && inputStream.available() == 0 && tags.length() != 0) {
                ps = connection.prepareStatement("UPDATE posts SET text=?, tags=?, lastUpdated=? WHERE postID=" + postID + " AND userID=" + userID);
                ps.setString(1, text);
                ps.setString(2, tags);
                ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            } else if (text.length() == 0 && inputStream.available() != 0 && tags.length() == 0) {
                ps = connection.prepareStatement("UPDATE posts SET lastUpdated=? WHERE postID=" + postID + " AND userID=" + userID);
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                updateFile(postID, inputStream);
                return true;
            } else if (text.length() == 0 && inputStream.available() != 0 && tags.length() != 0) {
                ps = connection.prepareStatement("UPDATE posts SET tags=?, lastUpdated=? WHERE postID=" + postID + " AND userID=" + userID);
                ps.setString(1, tags);
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                updateFile(postID, inputStream);
            }  else if (text.length() == 0 && inputStream.available() == 0 && tags.length() != 0) {
                ps = connection.prepareStatement("UPDATE posts SET tags=?, lastUpdated=? WHERE postID=" + postID + " AND userID=" + userID);
                ps.setString(1, tags);
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            }   else {
                ps = connection.prepareStatement("UPDATE posts SET text=?, tags=?, lastUpdated=? WHERE postID=" + postID + " AND userID=" + userID);
                ps.setString(1, text);
                ps.setString(2, tags);
                ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                updateFile(postID, inputStream);
            }

            int i = ps.executeUpdate();

            if(i == 1) {
                System.out.println("updated post successfully");
                return true;
            }else {
                System.out.println("Not authorized to update post");
            }

        } catch (SQLException | IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    //update file in the files table
    public boolean updateFile(int postID, InputStream inputStream) {
        Connection connection = DBConnection.getConnection();

        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE files SET file=?, date=? WHERE fileID=" + postID);

            ps.setBlob(1, inputStream);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));

            int i = ps.executeUpdate();

            if(i == 1) {
                System.out.println("updated file successfully");
                return true;
            }else {
                System.out.println("Not authorized to update file");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    //delete a post from the post table.
    public boolean deletePost(int userID, int postID) {
        Connection connection = DBConnection.getConnection();

        try {
            Statement stmt = connection.createStatement();
            int i = stmt.executeUpdate("DELETE FROM posts WHERE postID=" + postID + " AND userID=" + userID);

            if(i == 1) {
                System.out.println("deleted post successfully");
                deleteFile(postID);
                return true;
            }else {
                System.out.println("Not authorized to delete post");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }

        return false;
    }


    //delete a file from the files table.
    public boolean deleteFile(int postID) {
        Connection connection = DBConnection.getConnection();

        try {
            Statement stmt = connection.createStatement();
            int i = stmt.executeUpdate("DELETE FROM files WHERE fileID=" + postID);

            if(i == 1) {
                System.out.println("deleted file successfully");
                return true;
            }else {
                System.out.println("Not authorized to delete file or no file found");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }

        return false;
    }

    //retrieve posts from posts table
    public ArrayList<Post> retrievePosts() {
        Connection connection = DBConnection.getConnection();
        ArrayList<Post> retrievedPosts = new ArrayList<Post>();
        int index = 0;
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM posts");

            while(rs.next()) {
//                Blob file = rs.getBlob("attachmentSource");
//                InputStream binaryStream = file.getBinaryStream();

                Post post = new Post(rs.getInt("userID"), rs.getString("text"), null, (rs.getTimestamp("date")), rs.getString("tags"), null, 0, null);
                post.setLastUpdated(rs.getTimestamp("lastUpdated"));
                post.setPostID(rs.getInt("postID"));

                retrievedPosts.add(post);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }
        return retrievedPosts;
    }

    //retrieve posts based on a search for user
    public ArrayList<Post> searchPosts(String username) {

        int searchedUserID;
        Connection connection = DBConnection.getConnection();
        ArrayList<Post> listOfSearchedPosts = new ArrayList<Post>();

        try {
            Statement statement = connection.createStatement();
            ResultSet query1 = statement.executeQuery("SELECT userID FROM users WHERE username="+"\""+username+"\"");

            if(query1.next()){
                searchedUserID = query1.getInt("userID");
                ResultSet query2 = statement.executeQuery("SELECT * FROM posts WHERE userID=" + searchedUserID);

                while (query2.next()) {
                    Post userPost = new Post(query2.getInt("postID"), query2.getInt("userID"), query2.getString("text"), null, (query2.getTimestamp("date")), query2.getString("tags"));
                    listOfSearchedPosts.add(userPost);
                }
            }

        }catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try{
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }
        return listOfSearchedPosts;
    }

    //retrieve post based on a date range
    public ArrayList<Post> searchByDates (String fromDate, String toDate){

        Connection connection = DBConnection.getConnection();
        ArrayList<Post> listOfDatePosts = new ArrayList<Post>();

        try{
            Statement statement = connection.createStatement();
            ResultSet query3 = statement.executeQuery("SELECT * FROM posts WHERE date BETWEEN "+"\""+fromDate+"\""+"AND"+"\""+toDate+"\"");

            while(query3.next()){
                Post datePost = new Post(query3.getInt("postID"), query3.getInt("userID"), query3.getString("text"), null, (query3.getTimestamp("date")), query3.getString("tags"));
                listOfDatePosts.add(datePost);
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            try{
                connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        return listOfDatePosts;
    }

    // retrieve post based on tags
    public ArrayList<Post> searchByTags (String tag){

        Connection connection = DBConnection.getConnection();
        ArrayList<Post> listsOfTagPosts = new ArrayList<Post>();

        try{
            Statement statement = connection.createStatement();
            ResultSet query4 = statement.executeQuery("SELECT * FROM posts WHERE tags="+"\""+tag+"\"");

            while(query4.next()){
                Post tagPost = new Post(query4.getInt("postID"), query4.getInt("userID"), query4.getString("text"), null, (query4.getTimestamp("date")), query4.getString("tags"));
                listsOfTagPosts.add(tagPost);
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            try{
                connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        return listsOfTagPosts;
    }

    // retrieve posts based on a search of user, tag and date range
    public ArrayList<Post> searchAll (String username, String fromDate, String toDate, String tag){

     int searchedUserID;
     Connection connection = DBConnection.getConnection();
     ArrayList<Post> listOfAllSearchedPosts = new ArrayList<Post>();

     try{
         Statement statement = connection.createStatement();
         ResultSet query5 = statement.executeQuery("SELECT userID FROM users WHERE username="+"\""+username+"\"");

         if(query5.next()){
             searchedUserID = query5.getInt("userID");
             ResultSet query6 = statement.executeQuery("SELECT * FROM posts WHERE date BETWEEN "+"\""+fromDate+"\""+"AND"+"\""+toDate+"\""+" AND userID="+searchedUserID+" AND tags="+"\""+tag+"\"");

             while (query6.next()) {
                 Post searchedPost = new Post(query6.getInt("postID"), query6.getInt("userID"), query6.getString("text"), null, (query6.getTimestamp("date")), query6.getString("tags"));
                 listOfAllSearchedPosts.add(searchedPost);
             }
         }
     } catch (SQLException e){
         e.printStackTrace();
     } finally {
         try{
             connection.close();
         }catch (SQLException e){
             e.printStackTrace();
         }
     }
        return listOfAllSearchedPosts;
    }

    //retrieve files from files table and bind them to posts
    public ArrayList<Post> retrieveFiles(ArrayList<Post> posts) {
        Connection connection = DBConnection.getConnection();
        int index = 0;
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs2= stmt.executeQuery("SELECT * FROM files");

            while(rs2.next()) {
                int id = rs2.getInt("fileID");
                Blob file = rs2.getBlob("file");
                InputStream binaryStream = file.getBinaryStream();

                for (int i = 0; i < posts.size(); i++) {
                    if(posts.get(i).getPostID() == id ) {
                        posts.get(i).setAttachment(binaryStream);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }
        return posts;
    }

    //sets userID generated by DB for a specific user
    public void retrieveUserID (User user) {
        Connection connection = DBConnection.getConnection();

        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT userID FROM users WHERE username=" + user.getUsername());

            if(rs.next())
            {
                user.setUserID(rs.getInt("userID"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }
    }

    // retrieve the username with userID
    public String retrieveUsername (int userID) {
        Connection connection = DBConnection.getConnection();
        String username = "";
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT username FROM users WHERE userID=" + userID);

            if(rs.next())
            {
                username = rs.getString("username");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }
        return username;
    }

    //sets postID generated by DB for a specific post
    public int retrievePostID (Post post) {
        Connection connection = DBConnection.getConnection();
        int id = 0;
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT postID FROM posts ORDER BY postID DESC LIMIT 1");

            if(rs.next())
            {
                id = rs.getInt("postID");
                System.out.println(id);
                post.setPostID(id);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }
        return id;
    }

    public User verifyPassword(User user){
        Connection connection = DBConnection.getConnection();
        String password = user.getPassword();
        String username = user.getUsername();
        User returnUser = null;
        try {
            Statement stmt = connection.createStatement();
//            ResultSet rs = stmt.executeQuery("SELECT userID, email FROM users WHERE username = '" +username + "' AND password = '" + password + "';" );
            ResultSet rs = stmt.executeQuery("SELECT userID FROM users WHERE username = '" +username + "' AND password = '" + password + "';" );

            String ans;
            if(rs.next())
            {
                returnUser = new User(username,password);
                returnUser.setUserID(rs.getInt("userID"));
//                returnUser.setEmail(rs.getNString("email"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
                return returnUser;
            } catch(SQLException e){
                e.printStackTrace();
                return returnUser;
            }
        }

    }

//    public String getFileName(int postID, int attachmentID){
//        String fileName = null;
//        Connection connection = DBConnection.getConnection();
//
//       /* try {
//            PreparedStatement ps = null;
//            if (text.length() != 0 && inputStream.available() == 0 && tags.length() == 0) {
//                ps = connection.prepareStatement("UPDATE posts SET text=?, lastUpdated=? WHERE postID=" + postID + " AND userID=" + userID);
//                ps.setString(1, text);
//                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
//            } else if (text.length() != 0 && inputStream.available() != 0 && tags.length() == 0) {
//
//            }*/
//       try{
//           PreparedStatement ps = null;
//           ps = connection.prepareStatement("select")
//       }catch(SQLException e){
//           e.printStackTrace();
//       }
//        String sql = "select attachment_name from attachments where attachment_ID=? and post_ID=?";
//
//        ResultSet rs = null;
//        try{
//            ps.setInt(1,attachmentID);
//            ps.setInt(2,postID);
//
//            rs = ps.executeQuery();
//            while (rs.next()){
//                fileName = rs.getString("attachment_name");
//            }
//
//        }catch(SQLException e){
//            e.printStackTrace();
//        }finally {
//            util.close();
//        }
//        return fileName;
//    }
}