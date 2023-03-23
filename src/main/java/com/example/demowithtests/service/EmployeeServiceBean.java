package com.example.demowithtests.service;

import com.example.demowithtests.domain.Employee;
import com.example.demowithtests.domain.Photo;
import com.example.demowithtests.repository.Repository;
import com.example.demowithtests.util.*;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
@org.springframework.stereotype.Service
public class EmployeeServiceBean implements EmployeeService {
    private final Repository repository;

    @SneakyThrows
    @Override
    public Employee create(Employee employee) {
        if (repository.findEmployeeByEmail(employee.getEmail()) == null) {
            if (employee.getEmail() == null) {
                throw new EmailAbsentException();
            }
            return repository.save(employee);
        }
        return repository.save(employee);
    }

    @Override
    public List<Employee> getAll() {
        if (repository.findAll().size() > 0) {
            if (repository.findAll().size() == repository.findEmployeeByIsDeletedIsTrue().size()) {
                throw new ListWasDeletedException();
            }
            return repository.findAll();
        }
        throw new ListHasNoAnyElementsException();

    }

    @Override
    public Employee getById(String id) {
        log.debug("getById() - start: id = {}", id);
        try {
            Integer employeeId = Integer.parseInt(id);
            Employee employee = repository.findById(employeeId)
                    .orElseThrow(IdIsNotExistException::new);
            if (employee.getIsDeleted()) {
                throw new ResourceWasDeletedException();
            }
            log.debug("getById() - end: employee = {}", employee);
            return employee;
        } catch (NumberFormatException exception) {
            log.debug("getById() - end: employee = {}", exception);
            throw new WrongDataException();
        }

    }

    @SneakyThrows
    @Override
    public Employee updateById(Integer id, Employee employee) {
        log.debug("updateById() started");
        Employee employeeUpdating = repository.findById(id)
                .map(entity -> {
                    entity.setName(employee.getName());
                    entity.setEmail(employee.getEmail());
                    entity.setCountry(employee.getCountry());
                    return repository.save(entity);
                })
                .orElseThrow(UserIsNotExistException::new);
        log.debug("updateById() completed for id = " + id);
        return employeeUpdating;
    }

    @Override
    public void removeById(Integer id) {
        log.debug("removeById() started for employee id = " + id);
        Employee employee = repository.findById(id)
                .orElseThrow(IdIsNotExistException::new);
        if (employee.getDeleted()) throw new UserAlreadyDeletedException();
        employee.setIsDeleted(true);
        repository.save(employee);
        log.debug("removeById() completed: employee with id = " + id + " removed");
    }

    @Override
    public void removeAll() {
        log.debug("removeAll() started");
        if (repository.findAll().size() > 0) {
            if (repository.findAll().size() == repository.findEmployeeByIsDeletedIsTrue().size()) {
                throw new ListWasDeletedException();
            }
            List<Employee> base = repository.findAll();
            for (Employee employee : base) {
                employee.setIsDeleted(true);
            }
            log.debug("removeAll() completed: all employees were deleted");
        }
        throw new ListHasNoAnyElementsException();
    }


    public void mailSender(List<String> emails, String text) {
        log.info("Emails sended");
    }

    @Override
    public List<Employee> sendEmailByCountry(String country, String text) {
        log.debug("sendEmailByCountry() started");
        List<Employee> employees = repository.findEmployeeByCountry(country);
        log.debug("sendEmailByCountry() get list of employees from " + country);
        mailSender(getterEmailsOfEmployees(employees), text);
        log.debug("sendEmailByCountry() completed: for all employees from " + country + " get emails");
        return employees;
    }

    public List<Employee> sendEmailByCity(String citiesString, String text) {
        log.debug("sendEmailByCity() started");
        String[] citiesArray = citiesString.split(",");
        List<String> citiesList = Arrays.asList(citiesArray);
        List<Employee> employees = new ArrayList<>();
        for (String city : citiesList) {
            List<Employee> employeesByCity = repository.findEmployeeByAddresses(city);
            employees.addAll(employeesByCity);
        }
        mailSender(getterEmailsOfEmployees(employees), text);
        log.debug("sendEmailByCity() completed: all employees from citites: " + citiesString + ", get emails");
        return employees;
    }

    @Override
    public void fillingDataBase(String quantityString) {
        log.debug("fillingDataBase() started");
        int quantity = Integer.parseInt(quantityString);
        for (int i = 0; i <= quantity; i++) {
            repository.save(createrEmployee("name", "country", "email"));
        }
        log.debug("fillingDataBase() completed: database was filling by " + quantityString + " employees");
    }


    @Override
    public void updaterByCountryFully(String countries) {
        log.debug("updaterByCountryFully() started");
        List<Employee> employees = repository.findAll();
        for (Employee employee : employees) {
            employee.setCountry(randomCountry(countries));
            repository.save(employee);
        }
        log.debug("updaterBycountryFully() completed");
    }

    @Override
    @Transactional
    public void updaterByCountrySmart(String countries) {
        log.debug("updaterByCountrySmart() started");
        List<Employee> employees = repository.findAll();
        for (Employee employee : employees) {
            String newCountry = randomCountry(countries);
            if (!employee.getCountry().equals(newCountry)) {
                employee.setCountry(newCountry);
                repository.save(employee);
            }
        }
        log.debug("updaterByCountrySmart() completed");
    }

    @Override
    public List<Employee> processor() {
        log.debug("processor() for replacing nulls started");
        List<Employee> replaceNull = repository.findEmployeeByIsDeletedNull();
        log.debug("replaced nulls for employees: " + replaceNull);
        for (Employee emp : replaceNull) {
            emp.setIsDeleted(Boolean.FALSE);
        }
        log.debug("processor() for replacing nulls completed");
        return repository.saveAll(replaceNull);
    }

    @Override
    public String randomCountry(String countriesString) {
        log.debug("randomCountry() started");
        List<String> countries = List.of(countriesString.split(","));
        log.debug("spliting of string countries in list completed");
        int randomIndex = (int) (Math.random() * countries.size());
        log.debug("randomCountry() completed");
        return countries.get(randomIndex);
    }

    private static List<String> getterEmailsOfEmployees(List<Employee> employees) {
        log.debug("getterEmailsOfEmployees() started");
        List<String> emails = new ArrayList<>();
        for (Employee employee : employees) {
            emails.add(employee.getEmail());
        }
        log.debug("getterEmailsOfEmployees() completed: list has size of " + emails.size() + " employees");
        return emails;
    }

    @Override
    public Employee createrEmployee(String name, String country, String email) {
        log.debug("createrEmployee() completed");
        return new Employee(name, country, email);
    }

    @Override
    public void emailSenderPhotoChange() {
        log.debug("emailSenderPhotoChange() started");
        Duration interval = Duration.ofDays(365);
        List<Employee> employees = repository.findAll();
        String text = "You should update your employee photo";
        List<Employee> employeesForChanges = new ArrayList<>();
        Iterator<Employee> iterator = employees.iterator();
        while (iterator.hasNext()) {
            Employee employee = iterator.next();
            if (employee.getPhotos().isEmpty()) {
                employeesForChanges.add(employee);
                iterator.remove();
            } else {
                Set<Photo> photos = employee.getPhotos();
                Set<Photo> oldPhotos = photos.stream()
                        .filter(photo -> Optional.ofNullable(photo.getCreatedDate())
                                .map(createdDate -> Duration.between
                                        (createdDate.toInstant(), Instant.now()).compareTo(interval) >= 0)
                                .orElse(false))
                        .collect(Collectors.toSet());
                if (!oldPhotos.isEmpty()) {
                    employeesForChanges.add(employee);
                }
            }
        }
        log.debug("emailSenderPhotoChange() completed");
        mailSender(getterEmailsOfEmployees(employeesForChanges), text);
    }

    @Override
    public void emailSenderWhoMovedFromUkraine(String country) {
        log.debug("emailSenderWhoMovedFromUkraine() started");
        String text = "We won the war, please, come back home";
        List<Employee> employeesFromUkraine =
                repository.findEmployeeByCountry(country);
        log.debug("list of employees from " + country + " received");
        List<Employee> employeesMovedFromUkraine = employeesFromUkraine.stream()
                .filter(employee -> employee.getAddresses().stream()
                        .anyMatch(address -> address.getCountry().equals(country)
                                && !address.getAddressHasActive()))
                .collect(Collectors.toList());
        log.debug("list of employees temporary moved from " + country + " received");
        mailSender(getterEmailsOfEmployees(employeesMovedFromUkraine), text);
        log.debug("emailSenderWhoMovedFromUkraine() completed: sent " +
                employeesMovedFromUkraine.size() + " emails");
    }
}