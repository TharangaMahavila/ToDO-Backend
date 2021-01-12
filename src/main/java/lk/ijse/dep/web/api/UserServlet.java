package lk.ijse.dep.web.api;

import dto.UserDTO;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import util.AppUtil;

import javax.crypto.SecretKey;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

/**
 * @author:Tharanga Mahavila <tharangamahavila@gmail.com>
 * @since : 2021-01-11
 **/
@WebServlet(name = "UserServlet",urlPatterns = {"/api/v1/users/*","/api/v1/auth"})
public class UserServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        Jsonb jsonb = JsonbBuilder.create();
        UserDTO userDTO = jsonb.fromJson(request.getReader(), UserDTO.class);
        if(userDTO.getUsername().isEmpty() || userDTO.getUsername().length()<3 || userDTO.getPassword().isEmpty() || userDTO.getUsername()==null || userDTO.getPassword()==null){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try(Connection connection = cp.getConnection()){
            if(request.getServletPath().equals("/api/v1/auth")){
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM user WHERE username=?");
                pstm.setObject(1,userDTO.getUsername());
                ResultSet resultSet = pstm.executeQuery();
                if(resultSet.next()){
                    String sha256Hex = DigestUtils.sha256Hex(userDTO.getPassword());
                    if(sha256Hex.equals(resultSet.getString("password"))){

                        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(AppUtil.getAppSecretKey()));
                        String jws = Jwts.builder()
                                .setIssuer("ijse")
                                .setExpiration(new Date(System.currentTimeMillis()+(1000*10)))
                                .setIssuedAt(new Date())
                                .claim("name", userDTO.getUsername())
                                .signWith(key)
                                .compact();
                               response.setContentType("text/plain");
                               response.getWriter().println(jws);
                    }else {
                        System.out.println("horek");
                    }
                }else{
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }
            }else {
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM user WHERE username=?");
                pstm.setObject(1, userDTO.getUsername());
                if (pstm.executeQuery().next()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                pstm = connection.prepareStatement("INSERT INTO user VALUES (?,?)");
                pstm.setObject(1, userDTO.getUsername());
                final String sha256Hex = DigestUtils.sha256Hex(userDTO.getPassword());
                pstm.setObject(2, sha256Hex);
                if (pstm.executeUpdate() > 0) {
                    response.setStatus(HttpServletResponse.SC_CREATED);
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        } catch (JsonbException ex){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
        catch (SQLException throwables) {
            throwables.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        try {
            Connection connection = cp.getConnection();
            System.out.println(connection);
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        Jsonb jsonb = JsonbBuilder.create();
        UserDTO user = jsonb.fromJson(req.getReader(),UserDTO.class);
        String username = req.getPathInfo().replace("/","");
        if(user.getUsername()==null || user.getPassword()==null || user.getUsername().isEmpty() ||
        user.getPassword().isEmpty() || !user.getUsername().equals(username)){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try(Connection connection = cp.getConnection()) {
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM user WHERE username=?");
            pstm.setObject(1,username);
            if(!pstm.executeQuery().next()){
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String password = DigestUtils.sha256Hex(user.getPassword());
            pstm = connection.prepareStatement("UPDATE user SET password = ? WHERE username =?");
            pstm.setObject(1,password);
            pstm.setObject(2,user.getUsername());
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
        Jsonb jsonb = JsonbBuilder.create();
        UserDTO user = jsonb.fromJson(req.getReader(), UserDTO.class);
        String username = req.getPathInfo().replace("/", "");
        if(user.getUsername()==null || user.getPassword()==null || user.getUsername().isEmpty() ||
                user.getPassword().isEmpty() || !user.getUsername().equals(username)){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try(Connection connection = cp.getConnection()) {
            PreparedStatement pstm = connection.prepareStatement("SELECT  * FROM user WHERE username=?");
            pstm.setObject(1,username);
            ResultSet rs = pstm.executeQuery();
            if(!rs.next()){
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }else{
                String password = DigestUtils.sha256Hex(user.getPassword());
                if(!rs.getString("password").equals(password)){
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
            pstm =  connection.prepareStatement("DELETE FROM user WHERE username=?");
            pstm.setObject(1,user.getUsername());
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
}
