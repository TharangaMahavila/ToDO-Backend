package lk.ijse.dep.web.api;

import dto.ToDoItemDTO;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import org.apache.commons.dbcp2.BasicDataSource;
import util.Priority;
import util.Status;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author:Tharanga Mahavila <tharangamahavila@gmail.com>
 * @since : 2021-01-11
 **/
@WebServlet(name = "TodoServlet",urlPatterns = "/api/v1/items/*")
public class TodoServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        Jsonb jsonb = JsonbBuilder.create();
        ToDoItemDTO item = jsonb.fromJson(request.getReader(), ToDoItemDTO.class);
        if(item.getId()!=null || item.getText()==null || item.getUsername()==null ||
        item.getText().trim().isEmpty() || item.getUsername().trim().isEmpty()){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try(Connection connection = cp.getConnection()) {
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM user WHERE username=?");
            pstm.setObject(1,item.getUsername());
            if(!pstm.executeQuery().next()){
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain");
                response.getWriter().println("Invalid User");
            }
            pstm = connection.prepareStatement("INSERT INTO todo_item(`text`,`priority`,`status`,`username`) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            pstm.setObject(1,item.getText());
            pstm.setObject(2,item.getPriority().toString());
            pstm.setObject(3,item.getStatus().toString());
            pstm.setObject(4,item.getUsername());
            if(pstm.executeUpdate()>0){
                response.setStatus(HttpServletResponse.SC_CREATED);
                ResultSet generatedKeys = pstm.getGeneratedKeys();
                generatedKeys.next();
                int generatedId = generatedKeys.getInt(1);
                item.setId(generatedId);
                response.setContentType("application/json");
                response.getWriter().println(jsonb.toJson(item));
            }else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (JsonbException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
        catch (SQLException throwables) {
            throwables.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        /*request.setAttribute("user","tharanga");*/
        Jsonb jsonb = JsonbBuilder.create();
        if(request.getPathInfo() == null || request.getPathInfo().equals("/")){
            try(Connection connection = cp.getConnection()) {
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM todo_item WHERE username=?");
                pstm.setObject(1,request.getAttribute("user"));
                ResultSet resultSet = pstm.executeQuery();
                List<Object> items =  new ArrayList<>();
                while (resultSet.next()){
                    items.add(new ToDoItemDTO(resultSet.getInt("id"),resultSet.getString("text"),
                            Priority.valueOf(resultSet.getString("priority")),
                            Status.valueOf(resultSet.getString("status")),
                            resultSet.getString("username")));
                }
                response.setContentType("application/json");
                response.getWriter().println(jsonb.toJson(items));
            } catch (SQLException | JsonbException throwables) {
                throwables.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }else{
            try (Connection connection = cp.getConnection()){
                int id = Integer.parseInt(request.getPathInfo().replace("/", ""));
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM todo_item WHERE id=? AND username=?");
                pstm.setObject(1,id);
                pstm.setObject(2,request.getAttribute("user"));
                ResultSet resultSet = pstm.executeQuery();
                if(resultSet.next()){
                    response.setContentType("application/json");
                    ToDoItemDTO item=new ToDoItemDTO(resultSet.getInt("id"),resultSet.getString("text"),
                            Priority.valueOf(resultSet.getString("priority")),
                            Status.valueOf(resultSet.getString("status")),
                            resultSet.getString("username"));
                    response.getWriter().println(jsonb.toJson(item));
                }else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }catch (NumberFormatException ex){
                System.out.println("Invalid id");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                ex.printStackTrace();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getPathInfo().replace("/", "");
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        Jsonb jsonb = JsonbBuilder.create();
        ToDoItemDTO item = jsonb.fromJson(req.getReader(), ToDoItemDTO.class);
        if(item.getId()!=null || item.getText()==null ||item.getUsername()==null ||
        item.getText().trim().isEmpty() || item.getUsername().trim().isEmpty() || !item.getId().equals(id)){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try(Connection connection = cp.getConnection()) {
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM todo_item WHERE id=? AND username=?");
            pstm.setObject(1,id);
            pstm.setObject(2,req.getAttribute("user"));
            if(!pstm.executeQuery().next()){
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            pstm = connection.prepareStatement("UPDATE todo_item SET text=?,priority=?,status=?, WHERE id=? AND username=?");
            pstm.setObject(1,item.getText());
            pstm.setObject(2,item.getPriority().toString());
            pstm.setObject(3,item.getStatus().toString());
            pstm.setObject(4,item.getId());
            pstm.setObject(5,item.getUsername());
            if(pstm.executeUpdate()>0){
                resp.setStatus(HttpServletResponse.SC_CREATED);
            }else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (JsonbException ex){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        try(Connection connection = cp.getConnection()) {
            int id = Integer.parseInt(req.getPathInfo().replace("/", ""));
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM todo_item WHERE id=? AND username=?");
            pstm.setObject(1,id);
            pstm.setObject(2,req.getAttribute("user"));
            if(!pstm.executeQuery().next()){
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            pstm = connection.prepareStatement("DELETE FROM todo_item WHERE id=? AND username=?");
            pstm.setObject(1,id);
            pstm.setObject(2,req.getAttribute("user"));
            if(pstm.executeUpdate()>0){
                resp.setStatus(HttpServletResponse.SC_CREATED);
            }else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (NumberFormatException ex){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }

    }
}
