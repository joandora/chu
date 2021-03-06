package com.github.scausidc.chu.user.servlet;

import java.io.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

import com.github.cuter44.nyafx.dao.*;
import com.github.cuter44.nyafx.servlet.*;
import com.github.cuter44.nyafx.crypto.*;
import static com.github.cuter44.nyafx.servlet.Params.*;
import com.alibaba.fastjson.*;

import static com.github.scausidc.chu.Constants.*;
import static com.github.scausidc.chu.nyafx.hj.Jsonizer.*;
import com.github.scausidc.chu.conf.*;
import com.github.scausidc.chu.user.model.*;
import com.github.scausidc.chu.user.dao.*;
import com.github.scausidc.chu.user.core.*;
import com.github.scausidc.chu.user.exception.*;

/** 登录
 * <pre style="font-size:12px">

   <strong>请求</strong>
   POST /user/login.api

   <strong>参数</strong>
   uid  :long   , uid
   pass :hex    , RSA 加密的 UTF-8 编码的用户登录密码.
   m    :hex    , 客户端 RSA 密钥, Modulus
   e    :hex    , 客户端 RSA 密钥, Public Exponent
   <i>密钥位数由 muuga.user.rsakeylength(=1024) 建议, 但不必要遵守, 只要零填充正确, nyafx-crypto 可以正确辨别位数</i>

   <strong>响应</strong>
   application/json class=user.model.User(with-secret)
   attributes refer to {@link Json#jsonizeUser(User) Json}

   <strong>例外</strong>
   parsed by {@link com.github.scausidc.chu.nyafx.servlet.ExceptionHandler ExceptionHandler}

   <strong>样例</strong>暂无
 * </pre>
 *
 */
@WebServlet("/user/login.api")
public class Login extends HttpServlet
{
    private static final String UID     = "uid";
    private static final String PASS    = "pass";
    private static final String M       = "m";
    private static final String E       = "e";

    protected UserDao userDao = UserDao.getInstance();
    protected Authorizer authorizer = Authorizer.getInstance();
    protected RSAKeyCache keyCache = RSAKeyCache.getInstance();
    protected RSACrypto rsa = RSACrypto.getInstance();

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        req.setCharacterEncoding("utf-8");

        try
        {
            Long        uid     = needLong(req, UID);
            byte[]      pass    = needByteArray(req, PASS);
            byte[]      m       = needByteArray(req, M);
            byte[]      e       = needByteArray(req, E);
            PrivateKey  key     = (PrivateKey)notNull(this.keyCache.get(uid));

            pass = this.rsa.decrypt(pass, key);

            this.userDao.begin();

            User u = this.authorizer.login(uid, pass);

            PublicKey publicKey = this.rsa.generatePublic(m, e);

            String encryptedSecret = this.rsa.byteToHex(
                this.rsa.encrypt(u.getSecret(), publicKey)
            );

            JSONObject json = (JSONObject)jsonize(null, u, JSONIZER_CONFIG_USER);

            json.put("secret", encryptedSecret);

            write(resp, json);

            this.userDao.commit();
        }
        catch (Exception ex)
        {
            req.setAttribute(ATTR_KEY_EXCEPTION, ex);
            req.getRequestDispatcher(URI_ERROR_HANDLER).forward(req, resp);
        }
        finally
        {
            this.userDao.close();
        }

        return;
    }
}
