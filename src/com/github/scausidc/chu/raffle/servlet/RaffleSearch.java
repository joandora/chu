package com.github.scausidc.chu.raffle.servlet;

import java.io.*;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

import org.hibernate.criterion.*;
import com.github.cuter44.nyafx.dao.*;
import static com.github.cuter44.nyafx.servlet.Params.*;

import static com.github.scausidc.chu.Constants.*;
import static com.github.scausidc.chu.nyafx.hj.Jsonizer.*;
import com.github.scausidc.chu.conf.*;
import com.github.scausidc.chu.raffle.model.*;
import com.github.scausidc.chu.raffle.dao.*;

/** 搜索 Raffle
 * <pre style="font-size:12px">

   <strong>请求</strong>
   GET/POST /raffle/search.api

   <strong>参数</strong>
   id       :long[]     , 逗号分隔, uid
   <i>分页</i>
   start    :int        , 返回结果的起始笔数, 缺省从 0 开始
   size     :int        , 返回结果的最大笔数, 缺省使用服务器配置
   <i>排序</i>
   by       :string             , 按该字段...
   order    :string=asc|desc    , 顺序|逆序排列

   <strong>响应</strong>
   application/json; array; class={com.github.scausidc.chu.raffle.model.Raffle Raffle}

   <strong>例外</strong>
   parsed by {@link com.github.scausidc.chu.nyafx.servlet.ExceptionHandler ExceptionHandler}

   <strong>样例</strong>暂无
 * </pre>
 *
 */
@WebServlet("/raffle/search.api")
public class RaffleSearch extends HttpServlet
{
    private static final String ID = "id";

    private static final String START = "start";
    private static final String SIZE = "size";
    private static final String ORDER = "order";
    private static final String BY = "by";

    private static final Integer defaultPageSize = Configurator.getInstance().getInt("nyafx.search.defaultpagesize", 20);

    protected RaffleDao raffleDao = RaffleDao.getInstance();

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        doPost(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        req.setCharacterEncoding("utf-8");

        try
        {
            List<Long>      id     = getLongList(req, ID);

            Integer     start   = getInt(req, START);
            Integer     size    = getInt(req, SIZE);
                        size    = size!=null?size:defaultPageSize;
            String      order   = getString(req, ORDER);
            String      by      = getString(req, BY);

            DetachedCriteria dc = DetachedCriteria.forClass(Raffle.class);

            if (id!=null)
                dc.add(Restrictions.in("id", id));

            if ("asc".equals(order))
                dc.addOrder(Order.asc(by));
            if ("desc".equals(order))
                dc.addOrder(Order.desc(by));

            this.raffleDao.begin();

            List<Raffle> l = (List<Raffle>)this.raffleDao.search(dc, start, size);

            this.raffleDao.commit();

            write(resp, jsonize(null, l, null));
        }
        catch (Exception ex)
        {
            req.setAttribute(ATTR_KEY_EXCEPTION, ex);
            req.getRequestDispatcher(URI_ERROR_HANDLER).forward(req, resp);
        }
        finally
        {
            this.raffleDao.close();
        }

        return;
    }
}
