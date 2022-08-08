package com.hcmus.wiberback.service.impl;

import com.hcmus.wiberback.model.dto.CarRequestDto;
import com.hcmus.wiberback.model.entity.mongo.CallCenter;
import com.hcmus.wiberback.model.entity.mongo.CarRequest;
import com.hcmus.wiberback.model.entity.mongo.Customer;
import com.hcmus.wiberback.model.enums.CarRequestStatus;
import com.hcmus.wiberback.model.enums.CarType;
import com.hcmus.wiberback.model.exception.CarRequestNotFoundException;
import com.hcmus.wiberback.model.exception.UserNotFoundException;
import com.hcmus.wiberback.repository.mongo.CallCenterRepository;
import com.hcmus.wiberback.repository.mongo.CarRequestRepository;
import com.hcmus.wiberback.repository.mongo.CustomerRepository;
import com.hcmus.wiberback.repository.redis.CarRequestRedisRepository;
import com.hcmus.wiberback.service.CarRequestMessageSender;
import com.hcmus.wiberback.service.CarRequestService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarRequestServiceImpl implements CarRequestService {

  private final CarRequestRepository carRequestRepository;
  private final CustomerRepository customerRepository;
  private final CallCenterRepository callCenterRepository;
  private final CarRequestMessageSender queueProducer;
  private final CarRequestRedisRepository carRequestRedisRepository;
  @Qualifier("carRequestQueue")
  private final Queue carRequestQueue;
  @Qualifier("carRequestStatusQueue")
  private final Queue carRequestStatusQueue;

  @Qualifier("locateRequestQueue")
  private final Queue locateRequestQueue;


  @Override
  public List<CarRequest> findAllCarRequests() {
    // TODO: Fix this later
    /*List<CarRequest> carRequests = new ArrayList<>();
    carRequestRedisRepository.findAll().forEach(carRequests::add);

    if (carRequests.isEmpty()) {
      carRequests = carRequestRepository.findAll();
      carRequestRedisRepository.saveAll(
          carRequests.stream().map(CarRequestRedis::new).collect(
              Collectors.toList()));
    }*/

    return carRequestRepository.findAll();
  }

  @Override
  public CarRequest findCarRequestById(String id) {
    return carRequestRepository.findById(id)
        .orElseThrow(() -> new CarRequestNotFoundException("Car request not found", id));
  }

  @Override
  public List<CarRequest> findCarRequestsByCustomerId(String customerId) {
    return carRequestRepository.findCarRequestsByCustomer(
        customerRepository
            .findById(customerId)
            .orElseThrow(() -> new UserNotFoundException("Customer not found", customerId)));
  }

  @Override
  public String saveOrUpdateCarRequest(CarRequestDto carRequestDto) {
    Customer customer;
    if (carRequestDto.getCustomerId() != null) {
      customer = customerRepository
          .findById(carRequestDto.getCustomerId())
          .orElseThrow(
              () -> new UserNotFoundException("Customer not found", carRequestDto.getCustomerId()));
    } else {
      customer = customerRepository.findCustomerByPhone(carRequestDto.getCustomerPhone())
          .orElseGet(() -> {
            Customer newCustomer = new Customer();
            newCustomer.setName(carRequestDto.getCustomerName());
            newCustomer.setPhone(carRequestDto.getCustomerPhone());
            return customerRepository.save(newCustomer);
          });
    }

    CarRequest carRequest;
    if (carRequestDto.getId() == null) {
      carRequest = CarRequest.builder().customer(customer)
          .pickingAddress(carRequestDto.getPickingAddress())
          .arrivingAddress(carRequestDto.getArrivingAddress())
          .lngPickingAddress(carRequestDto.getLngPickingAddress())
          .latPickingAddress(carRequestDto.getLatPickingAddress())
          .lngArrivingAddress(carRequestDto.getLngArrivingAddress())
          .latArrivingAddress(carRequestDto.getLatArrivingAddress())
          .status(carRequestDto.getStatus())
          .carType(CarType.valueOf(carRequestDto.getCarType())).build();
    } else {
      carRequest = carRequestRepository.findById(carRequestDto.getId())
          .orElseThrow(() -> new CarRequestNotFoundException("Car request not found",
              carRequestDto.getId()));
      carRequest.setPickingAddress(carRequestDto.getPickingAddress());
      carRequest.setArrivingAddress(carRequestDto.getArrivingAddress());
      carRequest.setLngPickingAddress(carRequestDto.getLngPickingAddress());
      carRequest.setLatPickingAddress(carRequestDto.getLatPickingAddress());
      carRequest.setLngArrivingAddress(carRequestDto.getLngArrivingAddress());
      carRequest.setLatArrivingAddress(carRequestDto.getLatArrivingAddress());
      carRequest.setStatus(carRequestDto.getStatus());
      carRequest.setCarType(CarType.valueOf(carRequestDto.getCarType()));
    }
    String carRequestId = carRequestRepository.save(carRequest).getId();

    carRequestDto.setId(carRequestId);
    // create a new car request to car-request-queue
    if (carRequestDto.getStatus() == CarRequestStatus.WAITING) {
      queueProducer.send(carRequestDto, carRequestQueue);
    } else if (carRequestDto.getStatus() == CarRequestStatus.ACCEPTED) {
      queueProducer.send(carRequestDto, carRequestStatusQueue);
    }

    // TODO: Fix this later
    /*CarRequestRedis carRequestRedis = new CarRequestRedis(carRequest);
    carRequestRedisRepository.save(carRequestRedis);*/
    return carRequestId;
  }

  @Override
  public String saveCarRequestCallCenter(CarRequestDto carRequestDto) {
    CallCenter callCenter = callCenterRepository
        .findById(carRequestDto.getCallCenterId())
        .orElseThrow(
            () -> new UserNotFoundException("Call-center not found",
                carRequestDto.getCallCenterId()));

    Customer customer = customerRepository.findCustomerByPhone(carRequestDto.getCustomerPhone())
        .orElseGet(() -> {
          Customer newCustomer = new Customer();
          newCustomer.setName(carRequestDto.getCustomerName());
          newCustomer.setPhone(carRequestDto.getCustomerPhone());
          return customerRepository.save(newCustomer);
        });

    List<CarRequest> oldCarRequests = carRequestRepository.findCarRequestByPickingAddress(carRequestDto.getPickingAddress());
    for (CarRequest carRequest : oldCarRequests) {
      if (carRequest.getLngPickingAddress() != null && carRequest.getLatPickingAddress() != null) {
        carRequestDto.setLngPickingAddress(carRequest.getLngPickingAddress());
        carRequestDto.setLatPickingAddress(carRequest.getLatPickingAddress());
        break;
      }
    }
    CarRequest carRequest;
    if(carRequestDto.getLngPickingAddress() != null && carRequestDto.getLatPickingAddress() != null) {
      carRequest = CarRequest.builder().customer(customer)
          .callCenter(callCenter)
          .pickingAddress(carRequestDto.getPickingAddress())
          .lngPickingAddress(carRequestDto.getLngPickingAddress())
          .latPickingAddress(carRequestDto.getLatPickingAddress())
          .status(carRequestDto.getStatus())
          .carType(CarType.valueOf(carRequestDto.getCarType())).build();

      carRequestRepository.save(carRequest);
      carRequestDto.setId(carRequest.getId());
      queueProducer.send(carRequestDto, carRequestQueue);
    } else {
      carRequest = CarRequest.builder().customer(customer)
          .callCenter(callCenter)
          .pickingAddress(carRequestDto.getPickingAddress())
          .status(carRequestDto.getStatus())
          .carType(CarType.valueOf(carRequestDto.getCarType())).build();

      carRequestRepository.save(carRequest);
      carRequestDto.setId(carRequest.getId());
      queueProducer.send(carRequestDto, locateRequestQueue);
    }
    return carRequest.getId();
  }
}
