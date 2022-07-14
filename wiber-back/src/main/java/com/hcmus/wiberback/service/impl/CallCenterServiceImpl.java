package com.hcmus.wiberback.service.impl;

import com.hcmus.wiberback.entity.dto.CallCenterRequestDto;
import com.hcmus.wiberback.entity.entity.Account;
import com.hcmus.wiberback.entity.entity.CallCenter;
import com.hcmus.wiberback.entity.exception.AccountNotFoundException;
import com.hcmus.wiberback.repository.AccountRepository;
import com.hcmus.wiberback.repository.CallCenterRepository;
import com.hcmus.wiberback.service.CallCenterService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CallCenterServiceImpl implements CallCenterService {
  private final CallCenterRepository callCenterRepository;
  private final AccountRepository accountRepository;

  @Override
  public List<CallCenter> getAllStaff() {
    return callCenterRepository.findAll();
  }

  @Override
  public String saveStaff(CallCenterRequestDto callCenterRequestDto) {
    Account account =
        accountRepository
            .findAccountByPhone(callCenterRequestDto.getPhone())
            .orElseThrow(() -> new AccountNotFoundException("Account not found",
                callCenterRequestDto.getPhone()));

    CallCenter staff = new CallCenter();
    staff.setName(callCenterRequestDto.getName());
    staff.setAccount(account);

    return callCenterRepository.save(staff).getId();
  }

  @Override
  public String updateStaff(CallCenterRequestDto callCenterRequestDto) {
    return null;
  }
}
