from persistence import *

import sys

def main(args : list):
    inputfilename : str = args[1]
    with open(inputfilename) as inputfile:
        for line in inputfile:
            splittedline : list = line.strip().split(", ")
            #TODO: apply the action (and insert to the table) if possible
            units = int(splittedline[1])
            product_id = int(splittedline[0])
            row = repo.products.find(id = product_id)
            quant = row[0].quantity
            if (units >= 0): # buy from supplier
                repo.products.update({"quantity" : (quant + units)}, {"id" : product_id})
                repo.activities.insert(Activitie(product_id, units, int(splittedline[2]), splittedline[3]))                
            else: # sell employee
                units = -1*units
                if (quant >= units):
                    repo.products.update({"quantity" : (quant - units)}, {"id" : product_id})
                    repo.activities.insert(Activitie(product_id, -1*units, int(splittedline[2]), splittedline[3]))

if __name__ == '__main__':
    main(sys.argv)