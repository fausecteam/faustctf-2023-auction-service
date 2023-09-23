#!/usr/bin/env python3

from pathlib import Path
import subprocess
from dataclasses import dataclass
from ctf_gameserver import checkerlib
import utils
import logging

root_dir = Path(__file__).absolute().parent.parent
classpath = root_dir / "service" / "bin"

server_registry_port = "12345"

# magic constant not exported by checkerlib
flagLookback = 5

@dataclass
class StoredFlag():
    name: str
    coupon: str

class AuctionServiceChecker(checkerlib.BaseChecker):
    server_address: str
    team_num: int

    stored_flags = None # Map[int, StoredFlag]

    def __init__(self, server_address:str, team_num:int):
        self.server_address = server_address
        self.team_num = team_num
        self.stored_flags = checkerlib.load_state('stored_flags')
        if self.stored_flags is None:
            self.stored_flags = {}
        logging.info(f"stored flags {self.stored_flags}")

    def check_immediate_result(self, stderr: str):
        if ">>>FAULTY<<<" in stderr:
            return checkerlib.CheckResult.FAULTY
        if ">>>DOWN<<<" in stderr:
            return checkerlib.CheckResult.DOWN
        if ">>>FLAG_NOT_FOUND<<<" in stderr:
            return checkerlib.CheckResult.FLAG_NOT_FOUND
        return None

    def place_flag(self, tick)->checkerlib.CheckResult:
        flag = checkerlib.get_flag(tick)

        args = [self.server_address,
                       server_registry_port,
                       flag]

        completedProcess = subprocess.run(["java", "-cp", classpath, "checker.PlaceFlag"] + args,
                                           capture_output=True)
        stdout = completedProcess.stdout.decode()
        stderr = completedProcess.stderr.decode()

        logging.info(stdout)
        logging.info(stderr)

        result = self.check_immediate_result(stderr)
        if result is not None:
            return result
        
        auctionName = stderr.split(">>>auctionName>>>")[1].split("<<<")[0]
        coupon = stderr.split(">>>coupon>>>")[1].split("<<<")[0]

        logging.info(f"auctionName {auctionName} coupon {coupon}")

        # publish auction name as flag-id
        checkerlib.set_flagid(auctionName)
        
        # store name and coupon in stored_flags
        self.stored_flags[tick] = StoredFlag(auctionName, coupon)
        # expire flags older than 5 ticks
        for flag_tick in list(self.stored_flags.keys()): # create copy to avoid 'dictionary changed size during iteration'
            if flag_tick < tick - flagLookback:
                del self.stored_flags[flag_tick]
        checkerlib.store_state('stored_flags', self.stored_flags)

        return checkerlib.CheckResult.OK


    def check_flag(self, tick)->checkerlib.CheckResult:
        flag = checkerlib.get_flag(tick)

        try:
            stored_flag = self.stored_flags[tick]
        except KeyError as ke:
            logging.info(f"Could not find the flag for tick {tick} in local cache.")
            return checkerlib.CheckResult.FLAG_NOT_FOUND
        
        auctionName = stored_flag.name
        coupon = stored_flag.coupon

        args = [self.server_address,
                        server_registry_port,
                        auctionName,
                        coupon]

        completedProcess = subprocess.run(["java", "-cp", classpath, "checker.CheckFlag"] + args,
                                            capture_output=True)
        stdout = completedProcess.stdout.decode()
        stderr = completedProcess.stderr.decode()

        logging.info(stdout)
        logging.info(stderr)

        result = self.check_immediate_result(stderr)
        if result is not None:
            return result

        found_flag = stderr.split(">>>flag>>>")[1].split("<<<")[0]

        if found_flag != flag:
            logging.info("flags do not match")
            return checkerlib.CheckResult.FLAG_NOT_FOUND
        return checkerlib.CheckResult.OK

    def check_service(self)->checkerlib.CheckResult:
        randomString = utils.generate_message()

        args = [self.server_address,
                       server_registry_port,
                       randomString]

        completedProcess = subprocess.run(["java", "-cp", classpath, "checker.CheckService"] + args,
                                           capture_output=True)
        stdout = completedProcess.stdout.decode()
        stderr = completedProcess.stderr.decode()

        logging.info(stdout)
        logging.info(stderr)

        result = self.check_immediate_result(stderr)
        if result is not None:
            return result

        return checkerlib.CheckResult.OK

if __name__ == '__main__':
    checkerlib.run_check(AuctionServiceChecker)
