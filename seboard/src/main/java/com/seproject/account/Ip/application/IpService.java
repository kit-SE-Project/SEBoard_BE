package com.seproject.account.Ip.application;

import com.seproject.account.Ip.domain.Ip;
import com.seproject.account.Ip.domain.IpType;
import com.seproject.account.Ip.domain.repository.IpRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Transactional(readOnly = true)
@Service
public class IpService {

    private final IpRepository ipRepository;

    private final List<Ip> adminIpList;
    private final List<Ip> spamIpList;

    public IpService(IpRepository ipRepository) {
        this.ipRepository = ipRepository;
        this.adminIpList = ipRepository.findAllByIpType(IpType.ADMIN);
        this.spamIpList = ipRepository.findAllByIpType(IpType.SPAM);
    }

    public List<Ip> findAll() {
        return ipRepository.findAll();
    }

    public List<Ip> findAllByIpType(IpType ipType) {
        if(ipType == IpType.SPAM) {
            if(spamIpList.isEmpty()) {
                List<Ip> allByIpType = ipRepository.findAllByIpType(IpType.SPAM);
                spamIpList.addAll(allByIpType);
            }

            return spamIpList;
        }

        if(ipType == IpType.ADMIN) {
            if(adminIpList.isEmpty()) {
                List<Ip> allByIpType = ipRepository.findAllByIpType(IpType.ADMIN);
                adminIpList.addAll(allByIpType);
            }

            return adminIpList;
        }

        throw new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_IP,null);
    }

    @Transactional
    public Long createIp(String ipAddress,IpType ipType) {
        Ip ip = Ip.builder()
                .ipAddress(ipAddress)
                .ipType(ipType)
                .build();

        ipRepository.save(ip);


        if(ipType == IpType.ADMIN) {
            adminIpList.clear();
        }

        if(ipType == IpType.SPAM) {
            spamIpList.clear();
        }

        return ip.getId();
    }

    @Transactional
    public void deleteIp(String ipAddress) {
        Ip ip = ipRepository.findByIpAddress(ipAddress)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_IP, null));
        ipRepository.delete(ip);

        IpType ipType = ip.getIpType();
        if(ipType == IpType.ADMIN) {
            adminIpList.clear();
        }

        if(ipType == IpType.SPAM) {
            spamIpList.clear();
        }
    }

    public boolean existIpAddress(String ipAddress) {

        for (Ip ip : adminIpList) {
            if(ipAddress.equals(ip.getIpAddress())) {
                return true;
            }
        }

        for (Ip ip : spamIpList) {
            if(ipAddress.equals(ip.getIpAddress())) {
                return true;
            }
        }

        return false;
    }

//    public Ip findIpByAddress(String address) {
//        return ipRepository.findByIpAddress(address)
//                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_IP,null));
//    }

}
