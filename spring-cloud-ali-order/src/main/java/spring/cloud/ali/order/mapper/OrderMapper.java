package spring.cloud.ali.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import spring.cloud.ali.order.model.Order;

import java.util.List;

@Mapper
public interface OrderMapper{

    @Select("SELECT * FROM orders WHERE user_id = #{userId}")
    List<Order> queryByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND order_no = #{orderNo}")
    Order queryByUserIdAndOrderNo(@Param("userId") Long userId, @Param("orderNo") String orderNo);
}
