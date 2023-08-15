package com.seproject.admin.ip.application;

import com.seproject.account.Ip.application.IpService;
import com.seproject.admin.ip.persitence.IpQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.seproject.admin.ip.controller.dto.IpDTO.*;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class AdminIpAppService {

    private final IpService ipService;
    private final IpQueryRepository ipQueryRepository;

    public Page<IpResponse> findAll(IpCondition condition, int page, int perPage) {
        PageRequest pageRequest = PageRequest.of(page, perPage);
        return ipQueryRepository.findAll(condition,pageRequest);

    }

    @Transactional
    public void createIp(CreateIpRequest request) {
        ipService.createIp(request.getIpAddress());
    }

    @Transactional
    public void delete(DeleteIpRequest request) {
        ipService.deleteIp(request.getIpAddress());
    }

}
