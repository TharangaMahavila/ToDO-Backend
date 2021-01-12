package filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lk.ijse.dep.web.api.TodoServlet;
import util.AppUtil;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author:Tharanga Mahavila <tharangamahavila@gmail.com>
 * @since : 2021-01-11
 **/
@WebFilter(filterName = "SecurityFilter",servletNames = {"TodoServlet"})
public class SecurityFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (req.getServletPath().equals("/api/v1/auth") && req.getMethod().equals("POST")) {
            chain.doFilter(req, res);
        } else {
            String authorization = req.getHeader("Authorization");
            if (!authorization.startsWith("Bearer")) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                String token = authorization.replace("Bearer", "");
                Jws<Claims> jws;

                try {
                    jws = Jwts.parserBuilder()  // (1)
                            .setSigningKey(AppUtil.getAppSecretKey())         // (2)
                            .build()                    // (3)
                            .parseClaimsJws(token); // (4)

                    // we can safely trust the JWT
                    req.setAttribute("user", jws.getBody().get("name"));
                    chain.doFilter(req, res);
                } catch (JwtException ex) {       // (5)

                    // we *cannot* use the JWT as intended by its creator
                    ex.printStackTrace();
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }
            }
        }
    }
}
