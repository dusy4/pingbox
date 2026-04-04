#!/usr/bin/env python3
"""
Docker Container Bootstrap Script for TriggerApp Backend

Simple subprocess-based approach - no extra dependencies required.
Just needs docker-compose CLI installed.

==============================================================================
QUICK REFERENCE
==============================================================================

# Full redeploy (stop → rebuild → start) - for backend code updates
python bootstrap.py redeploy

# Same but force clean build (no cache)
python bootstrap.py redeploy --no-cache

# Individual operations
python bootstrap.py down              # Stop containers
python bootstrap.py build             # Rebuild images
python bootstrap.py up                # Start containers
python bootstrap.py build up          # Chain: build then up
python bootstrap.py restart           # Restart running containers
python bootstrap.py logs -f           # Follow logs
python bootstrap.py status            # Show container status

# Target specific service (api, nginx, db, redis)
python bootstrap.py build --service api
python bootstrap.py restart --service api
python bootstrap.py logs --service api -f

# Dangerous operations
python bootstrap.py down -v           # Stop and remove volumes (DATA LOSS!)
python bootstrap.py down -v -i        # Stop, remove volumes and images

==============================================================================
"""

import argparse
import subprocess
import sys
from pathlib import Path
from typing import Optional, List

# Configuration
BACKEND_DIR = Path(__file__).parent / "backend"
COMPOSE_FILE = BACKEND_DIR / "docker-compose.yml"


class DockerError(Exception):
    """Custom exception for Docker operations."""
    pass


def run_command(cmd: list[str], cwd: Optional[Path] = None, check: bool = True,
                capture_output: bool = False) -> subprocess.CompletedProcess:
    """
    Execute a shell command with exception handling.
    
    Args:
        cmd: Command and arguments
        cwd: Working directory
        check: Raise exception on non-zero exit
        capture_output: Capture stdout/stderr
    
    Raises:
        DockerError: On command failure with helpful messages
    """
    print(f"Running: {' '.join(cmd)}")
    
    try:
        result = subprocess.run(
            cmd,
            cwd=cwd or BACKEND_DIR,
            check=False,
            capture_output=capture_output,
            text=True
        )
        
        if check and result.returncode != 0:
            error_msg = result.stderr if capture_output else "Command failed"
            
            # Helpful error messages for common issues
            if "Cannot connect to the Docker daemon" in error_msg:
                raise DockerError("Docker daemon is not running. Start it with: sudo systemctl start docker")
            elif "permission denied" in error_msg.lower():
                raise DockerError("Permission denied. Try: sudo python bootstrap.py ...")
            elif "docker-compose" in error_msg and "not found" in error_msg:
                raise DockerError("docker-compose not found. Install it first.")
            else:
                raise DockerError(f"Command failed: {error_msg}")
        
        return result
        
    except FileNotFoundError as e:
        if "docker-compose" in str(e) or "docker" in str(e):
            raise DockerError(f"Command not found: {e}. Install docker-compose.")
        raise DockerError(f"Command not found: {e}")
    except PermissionError as e:
        raise DockerError(f"Permission denied: {e}")
    except subprocess.TimeoutExpired as e:
        raise DockerError(f"Command timed out: {e}")


def containers_down(remove_volumes: bool = False, remove_images: bool = False):
    """Stop and optionally remove containers."""
    print("\n=== Stopping containers ===")
    
    try:
        cmd = ["docker-compose", "down"]
        
        if remove_volumes:
            cmd.append("-v")
            print("⚠️  Warning: Will remove volumes (data will be lost!)")
        
        if remove_images:
            cmd.extend(["--rmi", "all"])
            print("⚠️  Warning: Will remove images")
        
        run_command(cmd)
        print("✓ Containers stopped successfully.")
        
    except DockerError as e:
        print(f"✗ Failed to stop containers: {e}")
        raise


def containers_build(service: Optional[str] = None, no_cache: bool = False):
    """Rebuild Docker images."""
    print("\n=== Building images ===")
    
    try:
        # First, build the local API image since it uses Dockerfile
        if service is None or service == "api":
            print("\n--- Building API image from Dockerfile ---")
            cmd = [
                "docker", "build",
                "-t", "triggerapp-api:latest",
                "-f", "Dockerfile",
                "."
            ]
            if no_cache:
                cmd.append("--no-cache")
            run_command(cmd)
        
        # Build via docker-compose for any service-specific builds
        print("\n--- Building via docker-compose ---")
        cmd = ["docker-compose", "build"]
        
        if no_cache:
            cmd.append("--no-cache")
        
        if service:
            cmd.append(service)
        
        run_command(cmd)
        print("✓ Images built successfully.")
        
    except DockerError as e:
        print(f"✗ Build failed: {e}")
        raise


def containers_up(detach: bool = True, services: Optional[List[str]] = None):
    """Start containers."""
    print("\n=== Starting containers ===")
    
    try:
        cmd = ["docker-compose", "up"]
        
        if detach:
            cmd.append("-d")
        
        if services:
            cmd.extend(services)
        
        run_command(cmd)
        print("✓ Containers started successfully.")
        
    except DockerError as e:
        print(f"✗ Failed to start containers: {e}")
        raise


def containers_restart(services: Optional[List[str]] = None):
    """Restart running containers."""
    print("\n=== Restarting containers ===")
    
    try:
        cmd = ["docker-compose", "restart"]
        
        if services:
            cmd.extend(services)
        
        run_command(cmd)
        print("✓ Containers restarted successfully.")
        
    except DockerError as e:
        print(f"✗ Failed to restart containers: {e}")
        raise


def containers_logs(follow: bool = False, tail: Optional[int] = None, service: Optional[str] = None):
    """View container logs."""
    try:
        cmd = ["docker-compose", "logs"]
        
        if follow:
            cmd.append("-f")
        
        if tail:
            cmd.extend(["--tail", str(tail)])
        
        if service:
            cmd.append(service)
        
        run_command(cmd, capture_output=False)
        
    except DockerError as e:
        print(f"✗ Failed to get logs: {e}")
        raise


def containers_status():
    """Show container status."""
    print("\n=== Container Status ===")
    
    try:
        run_command(["docker-compose", "ps"])
    except DockerError as e:
        print(f"✗ Failed to get status: {e}")
        raise


def full_redeploy(no_cache: bool = False):
    """Full redeploy: down -> build -> up."""
    print("\n" + "="*50)
    print("FULL REDEPLOYMENT")
    print("="*50)
    
    try:
        containers_down()
        containers_build(no_cache=no_cache)
        containers_up(detach=True)
        
        print("\n" + "="*50)
        print("REDEPLOYMENT COMPLETE")
        print("="*50)
        containers_status()
        
    except DockerError as e:
        print(f"\n✗ Redeployment failed: {e}")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(
        description="Docker Container Bootstrap Script for TriggerApp Backend",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Full redeploy (down -> build -> up)
  python bootstrap.py redeploy

  # Same but clean build (no cache)
  python bootstrap.py redeploy --no-cache

  # Just stop containers
  python bootstrap.py down

  # Build and start
  python bootstrap.py build up

  # Rebuild specific service
  python bootstrap.py build --service api

  # Restart only
  python bootstrap.py restart

  # View logs
  python bootstrap.py logs --follow
        """
    )
    
    parser.add_argument(
        "action",
        nargs="+",
        choices=["down", "build", "up", "redeploy", "restart", "logs", "status"],
        help="Action(s) to perform on containers"
    )
    
    parser.add_argument(
        "--service",
        "-s",
        help="Specific service to target (for build/restart/logs)"
    )
    
    parser.add_argument(
        "--services",
        nargs="+",
        help="Specific services to start (for up command)"
    )
    
    parser.add_argument(
        "--no-cache",
        action="store_true",
        help="Build without cache"
    )
    
    parser.add_argument(
        "--remove-volumes",
        "-v",
        action="store_true",
        help="Remove volumes when stopping (DANGER: data loss!)"
    )
    
    parser.add_argument(
        "--remove-images",
        "-i",
        action="store_true",
        help="Remove images when stopping"
    )
    
    parser.add_argument(
        "--detach",
        "-d",
        action="store_true",
        default=True,
        help="Run containers in background (default: True)"
    )
    
    parser.add_argument(
        "--no-detach",
        action="store_true",
        help="Run containers in foreground"
    )
    
    parser.add_argument(
        "--follow",
        "-f",
        action="store_true",
        help="Follow log output"
    )
    
    parser.add_argument(
        "--tail",
        "-t",
        type=int,
        help="Number of lines to show from end of logs"
    )
    
    args = parser.parse_args()
    
    # Handle no-detach option
    detach = args.detach and not args.no_detach
    
    # Handle combined actions with global exception handling
    try:
        for action in args.action:
            if action == "redeploy":
                full_redeploy(no_cache=args.no_cache)
            
            elif action == "down":
                containers_down(
                    remove_volumes=args.remove_volumes,
                    remove_images=args.remove_images
                )
            
            elif action == "build":
                containers_build(
                    service=args.service,
                    no_cache=args.no_cache
                )
            
            elif action == "up":
                containers_up(
                    detach=detach,
                    services=args.services
                )
            
            elif action == "restart":
                services = args.services
                if not services and args.service:
                    services = [args.service]
                containers_restart(services=services)
            
            elif action == "logs":
                containers_logs(
                    follow=args.follow,
                    tail=args.tail,
                    service=args.service
                )
            
            elif action == "status":
                containers_status()
    
    except KeyboardInterrupt:
        print("\n\n⚠️  Operation interrupted by user.")
        sys.exit(130)
    except DockerError as e:
        print(f"\n✗ Operation failed: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"\n✗ Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
