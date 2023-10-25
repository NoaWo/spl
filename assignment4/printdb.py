from persistence import *

def print_table(dao_type, key_order):
    print (dao_type._table_name.capitalize())
    tuples = dao_type.find_all_order_list(key_order)
    for tup in tuples:
        print (tuple(t.decode("utf8") if isinstance(t, bytes) else t for t in tup))

def print_employees_report():
    print ("\nEmployees report")
    c = repo.employees._conn.cursor()
    query = '''SELECT employees.name, employees.salary, branches.location, COALESCE(SUM(products.price * -1 *activities.quantity), 0)
                FROM employees
                JOIN branches ON employees.branche = branches.id
                LEFT JOIN activities ON employees.id = activities.activator_id
                LEFT JOIN products ON activities.product_id = products.id
                GROUP BY employees.id
                ORDER BY employees.name ASC'''               
    c.execute(query)
    tuples = c.fetchall()
    for tup in tuples:
        print (' '.join(str(elem.decode("utf-8") if isinstance(elem, bytes) else elem) for elem in tup))

def print_activity_report():
    print ("\nActivities report")
    c = repo.activities._conn.cursor()
    query = '''SELECT activities.date, products.description, activities.quantity, employees.name, suppliers.name
                FROM activities 
                LEFT JOIN products ON activities.product_id = products.id
                LEFT JOIN employees ON activities.activator_id = employees.id
                LEFT JOIN suppliers ON activities.activator_id = suppliers.id
                ORDER BY activities.date ASC'''
    c.execute(query)
    tuples = c.fetchall()
    for tup in tuples:
        print (tuple(t.decode("utf8") if isinstance(t, bytes) else t for t in tup))

def main():
    #TODO: implement
    print_table(repo.activities, "date") # special case
    print_table(repo.branches, "id")
    print_table(repo.employees, "id")
    print_table(repo.products, "id")
    print_table(repo.suppliers, "id")
    print_employees_report()
    print_activity_report()    

if __name__ == '__main__':
    main()