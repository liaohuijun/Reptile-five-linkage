package com.github.liaohuijun.address.dao;

import com.github.liaohuijun.address.bean.Address;

import java.util.List;

/**
 * 地址DAO
  * (用一句话描述类的主要功能)
  * @author LIAO  
  * @date 2018年3月5日
 */
public interface AddressDao {

    public List<Address> findAllProvince();

    public List<Address> findByFather(String fatherCode);

    int insertOneAddress(Address address);

    void insertAddressPatch(List<Address> insertList);

}
