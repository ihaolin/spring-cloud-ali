package spring.cloud.ali.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import spring.cloud.ali.order.model.Order;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
